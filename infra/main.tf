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

# 0. 네트워크 설정 (VPC 및 Private IP)
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

# 2. Cloud SQL (MySQL) - db-f1-micro (최소 사양)
resource "random_id" "suffix" {
  byte_length = 4
}

resource "google_sql_database_instance" "master" {
  name                = "stiky-mysql-prod-${random_id.suffix.hex}"
  database_version    = "MYSQL_8_0"
  region              = var.region
  deletion_protection = true # 데이터 보호를 위해 켜둠

  settings {
    tier = "db-f1-micro"
    ip_configuration {
      ipv4_enabled    = false
      private_network = "projects/${var.project_id}/global/networks/default"
    }

    # 비용 절감 옵션 (필요시 활성화)
    # backup_configuration {
    #   enabled = false
    # }
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

# 3. Redis (VM 방식) - e2-micro (비용 최적화)
resource "google_compute_instance" "redis_vm" {
  name         = "stiky-redis-vm"
  machine_type = "e2-micro"
  zone         = "${var.region}-a"

  # 비용 절감을 위해 스팟 인스턴스 고려 가능 (현재는 표준 유지)
  # scheduling {
  #   preemptible       = true
  #   automatic_restart = false
  # }

  boot_disk {
    initialize_params {
      image = "cos-cloud/cos-stable"
      size  = 10              # 디스크 크기 최소화 (10GB)
      type  = "pd-standard"   # SSD 대신 표준 디스크 사용 (비용 절감)
    }
  }

  network_interface {
    network    = "default"
    subnetwork = "default"
    access_config {} # 외부 IP 필요 없음 (비용 절감) -> Cloud Run과 통신 위해 내부 IP만 있으면 됨
    # 단, 외부에서 접속해 디버깅하려면 access_config {} 가 필요할 수 있음.
    # 비용을 아끼려면 access_config {} 블록을 삭제하세요. (NAT Gateway 없으면 외부 인터넷 안 됨)
    # 지금은 유지하되 필요 없으면 삭제 추천.
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

# 4. Cloud Run (애플리케이션) - 비용 최적화 적용
resource "google_cloud_run_v2_service" "default" {
  name     = "stiky-api"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL" # LB 제거했으므로 모든 트래픽 허용

  deletion_protection = false

  template {
    scaling {
      min_instance_count = 0  # [중요] 0으로 설정하여 미사용 시 비용 0원
      max_instance_count = 5  # 과금 폭탄 방지
    }

    max_instance_request_concurrency = 80

    vpc_access{
      network_interfaces {
        network = "default"
        subnetwork = "default"
      }
      egress = "PRIVATE_RANGES_ONLY" # 내부 통신(DB, Redis)만 VPC로, 나머지는 인터넷으로
    }

    containers {
      image = "us-docker.pkg.dev/cloudrun/container/hello" # 실제 배포 시 Github Actions 등이 덮어씀

      resources {
        limits = {
          cpu    = "1000m"
          memory = "1Gi"
        }
        cpu_idle = true # [중요] 요청 처리 중에만 CPU 할당 (비용 절감)
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

# Output: Cloud Run 기본 URL 출력
output "cloud_run_url" {
  description = "Cloud Run의 기본 HTTPS URL입니다. 이 주소를 프론트엔드(Vercel) 환경변수에 설정하세요."
  value       = google_cloud_run_v2_service.default.uri
}
