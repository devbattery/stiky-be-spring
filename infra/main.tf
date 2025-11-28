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

# Secret Manager에 저장된 DB 비밀번호 값을 가져옴
data "google_secret_manager_secret_version" "db_pass" {
  secret = "DB_PASS"
}

# 0. VPC 네트워크 설정 (Private IP 사용을 위한 필수 설정)
# 구글 서비스용 내부 IP 대역 예약
resource "google_compute_global_address" "private_ip_address" {
  name          = "private-ip-address"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = "default"
}

# VPC와 구글 서비스 간 피어링 연결
resource "google_service_networking_connection" "private_vpc_connection" {
  network                 = "default"
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_address.name]
}

# 1. Artifact Registry (이미지 저장소)
resource "google_artifact_registry_repository" "repo" {
  location      = var.region
  repository_id = "stiky-repo"
  format        = "DOCKER"
}

# 2. Cloud SQL (MySQL)
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

  # [중요] 네트워크 연결이 끝난 뒤에 DB를 만들어야 에러가 안 남
  depends_on = [google_service_networking_connection.private_vpc_connection]
}

resource "random_id" "suffix" {
  byte_length = 4
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

# 3. Redis (Memorystore)
resource "google_redis_instance" "cache" {
  name           = "stiky-redis-prod"
  memory_size_gb = 1
  region         = var.region

  # Redis도 Private IP를 쓰므로 네트워크 연결에 의존성을 둠
  depends_on = [google_service_networking_connection.private_vpc_connection]
}

# 4. Cloud Run (애플리케이션)
resource "google_cloud_run_v2_service" "default" {
  name     = "stiky-api"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  deletion_protection = false

  template {
    vpc_access{
      network_interfaces {
        network = "default"
        subnetwork = "default"
      }
      egress = "ALL_TRAFFIC"
    }

    containers {
      # 나중에 GitHub Actions가 진짜 이미지로 덮어씌웁니다.
      image = "us-docker.pkg.dev/cloudrun/container/hello"

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
        value = google_redis_instance.cache.host
      }

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


# 1. 고정 IP 예약 (로드밸런서용)
resource "google_compute_global_address" "lb_ip" {
  name = "stiky-lb-ip"
}

# 2. Serverless NEG (Network Endpoint Group)
# 로드밸런서가 Cloud Run을 찾을 수 있게 해주는 그룹
resource "google_compute_region_network_endpoint_group" "cloudrun_neg" {
  name                  = "stiky-neg"
  network_endpoint_type = "SERVERLESS"
  region                = var.region

  cloud_run {
    service = google_cloud_run_v2_service.default.name
  }
}

# 3. 백엔드 서비스 (Backend Service)
# 로드밸런서가 트래픽을 NEG로 보낼 때의 설정
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

# 4. URL 맵 (URL Map)
# 어떤 주소로 들어오면 어디로 보낼지 결정 (여기서는 모든 요청을 백엔드로)
resource "google_compute_url_map" "lb_url_map" {
  name            = "stiky-url-map"
  default_service = google_compute_backend_service.lb_backend.id
}

# 5. 구글 관리형 SSL 인증서 (HTTPS)
# api.stiky.site 도메인에 대한 인증서를 자동 발급
resource "google_compute_managed_ssl_certificate" "lb_cert" {
  name = "stiky-api-cert"

  managed {
    domains = ["api.stiky.site"]
  }
}

# 6. HTTPS 프록시 (Target HTTPS Proxy)
# 인증서와 URL 맵을 연결
resource "google_compute_target_https_proxy" "lb_https_proxy" {
  name             = "stiky-https-proxy"
  url_map          = google_compute_url_map.lb_url_map.id
  ssl_certificates = [google_compute_managed_ssl_certificate.lb_cert.id]
}

# 7. 포워딩 룰 (Global Forwarding Rule)
# 전세계 어디서든 443 포트(HTTPS)로 들어오는 요청을 처리
resource "google_compute_global_forwarding_rule" "lb_forwarding_rule" {
  name       = "stiky-https-forwarding-rule"
  target     = google_compute_target_https_proxy.lb_https_proxy.id
  port_range = "443"
  ip_address = google_compute_global_address.lb_ip.id
}

# 8. HTTP -> HTTPS 리다이렉트용 프록시
# 80 포트로 들어오면 443으로 튕겨내는 설정 (필요하면 추가)

# 9. 생성된 IP 주소 출력 (나중에 Cloud DNS에 넣어야 함)
output "load_balancer_ip" {
  value = google_compute_global_address.lb_ip.address
}