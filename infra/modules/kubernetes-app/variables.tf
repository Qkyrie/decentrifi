locals {
  common_labels = {
    app  = var.app_name
    team = "decentrifi"
  }

  prometheus_annotations = {
    "prometheus.io/scrape" = "true"
    "prometheus.io/port" = tostring(var.container_port)
  }
}

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

variable "resources" {
  description = "Resource requests & limits"
  type = object({
    limits   = object({ cpu = string, memory = string })
    requests = object({ cpu = string, memory = string })
  })
  default = {
    limits   = { cpu = "0.5", memory = "512Mi" }
    requests = { cpu = "0.2", memory = "256Mi" }
  }
}

variable "env_vars" {
  description = "Environment variables"
  type = list(object({
    name = string
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

variable "liveness_probe" {
  description = "Liveness probe configuration"
  type = object({
    http_get = optional(object({
      path = string
      port = number
    }))
    tcp_socket = optional(object({
      port = number
    }))
    initial_delay_seconds = optional(number)
    period_seconds        = optional(number)
    timeout_seconds       = optional(number)
    success_threshold     = optional(number)
    failure_threshold     = optional(number)
  })
  default = {
    http_get = {
      path = "/health"
      port = 8080
    }
    initial_delay_seconds = 30
    period_seconds        = 10
  }
}

variable "readiness_probe" {
  description = "Readiness probe configuration"
  type = object({
    http_get = optional(object({
      path = string
      port = number
    }))
    tcp_socket = optional(object({
      port = number
    }))
    initial_delay_seconds = optional(number)
    period_seconds        = optional(number)
    timeout_seconds       = optional(number)
    success_threshold     = optional(number)
    failure_threshold     = optional(number)
  })
  default = {
    http_get = {
      path = "/health"
      port = 8080
    }
    initial_delay_seconds = 10
    period_seconds        = 5
  }
}