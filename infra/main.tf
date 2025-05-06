variable "github_repo" {
  description = "GitHub repository name (org/repo format)"
  type        = string
  default     = "qkyrie/decentrifi"
}

module "data_api" {
  source       = "./modules/kubernetes-app"
  app_name     = "data-api"
  namespace    = kubernetes_namespace.decentrifi-namespace.metadata.0.name
  github_repo  = var.github_repo
  container_port = 8080
  replicas     = 1
  
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
      name = "SERVER_PORT"
      value = "8080"
    }
  ]
}