variable "github_repo" {
  description = "GitHub repository name (org/repo format)"
  type        = string
  default     = "qkyrie/decentrifi"
}

module "data_api" {
  source         = "./modules/kubernetes-app"
  app_name       = "data-api"
  namespace      = kubernetes_namespace.decentrifi-namespace.metadata.0.name
  github_repo    = var.github_repo
  container_port = 8080
  replicas       = 1

  env_vars = [
    {
      name  = "KTOR_ENV"
      value = "production"
    },
    {
      name = "DB_JDBC_URL"
      value_from = {
        secret_key_ref = {
          name = "decentrifi-secrets"
          key  = "postgres-url"
        }
      }
    },
    {
      name = "DB_USERNAME"
      value_from = {
        secret_key_ref = {
          name = "decentrifi-secrets"
          key  = "postgres-username"
        }
      }
    },
    {
      name = "DB_PASSWORD"
      value_from = {
        secret_key_ref = {
          name = "decentrifi-secrets"
          key  = "postgres-password"
        }
      }
    },
    {
      name  = "SERVER_PORT"
      value = "8080"
    }
  ]
}

module "chainlayer" {
  source         = "./modules/kubernetes-app"
  app_name       = "chainlayer"
  namespace      = kubernetes_namespace.decentrifi-namespace.metadata.0.name
  github_repo    = var.github_repo
  container_port = 8080
  replicas       = 1

  env_vars = [
    {
      name = "ETH_RPC_URL"
      value_from = {
        secret_key_ref = {
          name = "decentrifi-secrets"
          key  = "eth-rpc-url"
        }
      }
    },
    {
      name  = "SERVER_PORT"
      value = "8080"
    },
    {
      name  = "SPRING_PROFILES_ACTIVE"
      value = "production"
    }
  ]
  
  # Example of custom probe configuration (Spring Boot actuator)
  liveness_probe = {
    http_get = {
      path = "/actuator/health"
      port = 8080
    }
    initial_delay_seconds = 60
    period_seconds        = 10
    failure_threshold     = 3
  }
  
  readiness_probe = {
    http_get = {
      path = "/actuator/health"
      port = 8080
    }
    initial_delay_seconds = 30
    period_seconds        = 5
    failure_threshold     = 3
  }
}

resource "kubernetes_cron_job_v1" "data-ingestion-hourly" {
  metadata {
    name      = "data-ingestion"
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
    labels = {
      app = "data-ingestion"
    }
  }

  spec {
    schedule                  = "0 * * * *"
    concurrency_policy = "Forbid"          # skip new run if old one active
    starting_deadline_seconds = 1800

    job_template {
      metadata {
        name      = "data-ingestion"
        namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
        labels = {
          app = "data-ingestion"
        }
      }
      spec {
        backoff_limit           = 3
        active_deadline_seconds = 5400

        template {
          metadata {
            labels = {
              app = "data-ingestion"
            }
          }
          spec {
            restart_policy = "OnFailure"
            termination_grace_period_seconds = 10

            container {
              name  = "data-ingestion"
              image = "ghcr.io/${var.github_repo}/data-ingestion:latest"
              env {
                name = "DB_PASSWORD"
                value_from {
                  secret_key_ref {
                    name = "decentrifi-secrets"
                    key  = "postgres-password"
                  }
                }
              }
              env {
                name = "DB_USERNAME"
                value_from {
                  secret_key_ref {
                    name = "decentrifi-secrets"
                    key  = "postgres-username"
                  }
                }
              }

              env {
                name = "ETH_RPC_URL"
                value_from {
                  secret_key_ref {
                    name = "decentrifi-secrets"
                    key  = "eth-rpc-url"
                  }
                }
              }

              env {
                name = "DB_JDBC_URL"
                value_from {
                  secret_key_ref {
                    name = "decentrifi-secrets"
                    key  = "postgres-url"
                  }
                }
              }

              resources {
                requests = {
                  cpu    = "100m"
                  memory = "512Mi"
                }
                limits = {
                  cpu    = "500m"
                  memory = "1.5Gi"
                }
              }
            }
          }
        }
      }
    }
  }
}