terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 5.0"
    }
  }
  backend "gcs" {
    bucket  = "stiky-terraform-state-wonjun"
    prefix  = "prod"
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Secret Manager 데이터 조회
data "google_secret_manager_secret_version" "db_pass" {
  secret = "DB_PASS"
}

# ---------------------------------------------------------
# 0. 네트워크 설정 (VPC 및 Private IP)
# ---------------------------------------------------------
resource "google_compute_global_address" "private_ip_address" {
  name          = "private-ip-address"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = "default"
}

resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = "default"
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_address.name]
}

# 1. Artifact Registry
resource "google_artifact_registry_repository" "repo" {
  location      = var.region
  repository_id = "stiky-repo"
  format        = "DOCKER"
}

# 2. Cloud SQL (MySQL)
resource "random_id" "suffix" {
  byte_length = 4
}

resource "google_sql_database_instance" "master" {
  name             = "stiky-mysql-prod-${random_id.suffix.hex}"
  database_version = "MYSQL_8_0"
  region           = var.region
  deletion_protection = true

  settings {
    tier = "db-f1-micro"
    ip_configuration {
      ipv4_enabled    = false
      private_network = "projects/${var.project_id}/global/networks/default"
    }
  }
  depends_on = [google_service_networking_connection.private_vpc_connection]
}

resource "google_sql_database" "database" {
  name     = "stiky"
  instance = google_sql_database_instance.master.name
}

resource "google_sql_user" "users" {
  name     = "wonjun"
  instance = google_sql_database_instance.master.name
  password = data.google_secret_manager_secret_version.db_pass.secret_data
}

# 3. Redis (VM)
resource "google_compute_instance" "redis_vm" {
  name         = "stiky-redis-vm"
  machine_type = "e2-micro"
  zone         = "${var.region}-a"

  boot_disk {
    initialize_params {
      image = "cos-cloud/cos-stable"
    }
  }

  network_interface {
    network    = "default"
    subnetwork = "default"
    access_config {}
  }

  metadata_startup_script = <<EOF
      docker rm -f redis || true
      docker run -d --name redis \
        -p 6379:6379 \
        --restart always \
        redis:alpine redis-server --protected-mode no
  EOF

  tags = ["redis-vm"]
  allow_stopping_for_update = true
}

resource "google_compute_firewall" "allow_redis" {
  name    = "allow-redis-internal"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["6379"]
  }

  source_ranges = ["10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"]
  target_tags   = ["redis-vm"]
}

# 4. Cloud Run (애플리케이션) - Auto Scaling 적용
resource "google_cloud_run_v2_service" "default" {
  name     = "stiky-api"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  deletion_protection = false

  template {
    scaling {
      min_instance_count = 1
      max_instance_count = 10
    }

    max_instance_request_concurrency = 80

    vpc_access{
      network_interfaces {
        network = "default"
        subnetwork = "default"
      }
      egress = "ALL_TRAFFIC"
    }

    containers {
      image = "us-docker.pkg.dev/cloudrun/container/hello"

      resources {
        limits = {
          cpu    = "2000m"
          memory = "2Gi"
        }
        cpu_idle = true
      }

      ports { container_port = 8080 }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "prod"
      }
      env {
        name  = "DB_HOST"
        value = google_sql_database_instance.master.private_ip_address
      }
      env {
        name  = "DB_NAME"
        value = google_sql_database.database.name
      }
      env {
        name  = "DB_USER"
        value = google_sql_user.users.name
      }
      env {
        name  = "REDIS_HOST"
        value = google_compute_instance.redis_vm.network_interface[0].network_ip
      }

      # Secrets
      env {
        name = "DB_PASS"
        value_source {
          secret_key_ref {
            secret  = "DB_PASS"
            version = "latest"
          }
        }
      }
      env {
        name = "JWT_SECRET"
        value_source {
          secret_key_ref {
            secret  = "JWT_SECRET"
            version = "latest"
          }
        }
      }
      env {
        name = "GOOGLE_CLIENT_ID"
        value_source {
          secret_key_ref {
            secret  = "GOOGLE_CLIENT_ID"
            version = "latest"
          }
        }
      }
      env {
        name = "GOOGLE_CLIENT_SECRET"
        value_source {
          secret_key_ref {
            secret  = "GOOGLE_CLIENT_SECRET"
            version = "latest"
          }
        }
      }
    }
  }

  lifecycle {
    ignore_changes = [
      client,
      client_version,
      template[0].containers[0].image,
      template[0].labels,
      template[0].annotations
    ]
  }
}

resource "google_cloud_run_v2_service_iam_member" "noauth" {
  location = google_cloud_run_v2_service.default.location
  name     = google_cloud_run_v2_service.default.name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# 5. 로드밸런서 (Load Balancer)
resource "google_compute_global_address" "lb_ip" {
  name = "stiky-lb-ip"
}

resource "google_compute_region_network_endpoint_group" "cloudrun_neg" {
  name                  = "stiky-neg"
  network_endpoint_type = "SERVERLESS"
  region                = var.region

  cloud_run {
    service = google_cloud_run_v2_service.default.name
  }
}

resource "google_compute_backend_service" "lb_backend" {
  name                  = "stiky-backend-service"
  protocol              = "HTTP"
  port_name             = "http"
  load_balancing_scheme = "EXTERNAL"
  timeout_sec           = 30

  backend {
    group = google_compute_region_network_endpoint_group.cloudrun_neg.id
  }
}

resource "google_compute_url_map" "lb_url_map" {
  name            = "stiky-url-map"
  default_service = google_compute_backend_service.lb_backend.id
}

resource "google_compute_managed_ssl_certificate" "lb_cert" {
  name = "stiky-api-cert"
  managed {
    domains = ["api.stiky.site"]
  }
}

resource "google_compute_target_https_proxy" "lb_https_proxy" {
  name             = "stiky-https-proxy"
  url_map          = google_compute_url_map.lb_url_map.id
  ssl_certificates = [google_compute_managed_ssl_certificate.lb_cert.id]
}

resource "google_compute_global_forwarding_rule" "lb_forwarding_rule" {
  name       = "stiky-https-forwarding-rule"
  target     = google_compute_target_https_proxy.lb_https_proxy.id
  port_range = "443"
  ip_address = google_compute_global_address.lb_ip.id
}

output "load_balancer_ip" {
  value = google_compute_global_address.lb_ip.address
}