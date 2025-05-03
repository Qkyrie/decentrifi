variable "app_name" {
  description = "Name of the application"
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace"
  type        = string
}

variable "github_repo" {
  description = "GitHub repository name (org/repo format)"
  type        = string
}

variable "container_port" {
  description = "Container port"
  type        = number
  default     = 8080
}

variable "replicas" {
  description = "Number of replicas"
  type        = number
  default     = 1
}

variable "cpu_limit" {
  description = "CPU limit"
  type        = string
  default     = "0.5"
}

variable "memory_limit" {
  description = "Memory limit"
  type        = string
  default     = "512Mi"
}

variable "cpu_request" {
  description = "CPU request"
  type        = string
  default     = "0.2"
}

variable "memory_request" {
  description = "Memory request"
  type        = string
  default     = "256Mi"
}

variable "env_vars" {
  description = "Environment variables"
  type = list(object({
    name  = string
    value = optional(string)
    value_from = optional(object({
      secret_key_ref = optional(object({
        name = string
        key  = string
      }))
    }))
  }))
  default = []
}

resource "kubernetes_service" "app_service" {
  metadata {
    namespace = var.namespace
    name      = var.app_name
    labels = {
      team = "decentrifi"
    }
    annotations = {
      "prometheus.io/scrape" : "true"
      "prometheus.io/port" : tostring(var.container_port)
    }
  }

  spec {
    selector = {
      app = var.app_name
    }

    port {
      name        = "http-traffic"
      port        = var.container_port
      target_port = var.container_port
      protocol    = "TCP"
    }
  }
}

resource "kubernetes_deployment" "app_deployment" {
  metadata {
    namespace = var.namespace
    name      = var.app_name
    labels = {
      app : var.app_name
    }
  }
  spec {
    replicas = var.replicas
    selector {
      match_labels = {
        app : var.app_name
      }
    }
    strategy {
      type = "RollingUpdate"
    }
    template {
      metadata {
        labels = {
          app : var.app_name
        }
      }
      spec {
        container {
          image             = "ghcr.io/${var.github_repo}/${var.app_name}:latest"
          name              = var.app_name
          image_pull_policy = "Always"
          port {
            container_port = var.container_port
          }
          
          dynamic "env" {
            for_each = var.env_vars
            content {
              name = env.value.name
              value = env.value.value
              dynamic "value_from" {
                for_each = env.value.value_from != null ? [env.value.value_from] : []
                content {
                  dynamic "secret_key_ref" {
                    for_each = value_from.value.secret_key_ref != null ? [value_from.value.secret_key_ref] : []
                    content {
                      name = secret_key_ref.value.name
                      key  = secret_key_ref.value.key
                    }
                  }
                }
              }
            }
          }
          
          resources {
            limits = {
              cpu    = var.cpu_limit
              memory = var.memory_limit
            }
            requests = {
              cpu    = var.cpu_request
              memory = var.memory_request
            }
          }
          liveness_probe {
            http_get {
              path = "/health"
              port = var.container_port
            }
            initial_delay_seconds = 30
            period_seconds        = 10
          }
        }
      }
    }
  }

  lifecycle {
    ignore_changes = [
      spec[0].template[0].metadata[0].annotations["kubectl.kubernetes.io/restartedAt"],
    ]
  }
}