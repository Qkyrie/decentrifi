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