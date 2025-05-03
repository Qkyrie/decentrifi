resource "kubernetes_service" "data-ingestion-service" {
  metadata {
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
    name      = "data-ingestion"
    labels = {
      team = "decentrifi"
    }
    annotations = {
      "prometheus.io/scrape" : "true"
      "prometheus.io/port" : "8080"
    }
  }

  spec {
    selector = {
      app = "data-ingestion"
    }

    port {
      name        = "http-traffic"
      port        = 8080
      target_port = 8080
      protocol    = "TCP"
    }
  }
}

resource "kubernetes_deployment" "data-ingestion-deployment" {
  metadata {
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
    name      = "data-ingestion"
    labels = {
      app : "data-ingestion"
    }
  }
  spec {
    replicas = "1"
    selector {
      match_labels = {
        app : "data-ingestion"
      }
    }
    strategy {
      type = "RollingUpdate"
    }
    template {
      metadata {
        labels = {
          app : "data-ingestion"
        }
      }
      spec {
        container {
          image             = "ghcr.io/${var.github_repo}/data-ingestion:latest"
          name              = "data-ingestion"
          image_pull_policy = "Always"
          port {
            container_port = 8080
          }
          env {
            name  = "KTOR_ENV"
            value = "production"
          }
          resources {
            limits = {
              cpu    = "0.5"
              memory = "512Mi"
            }
            requests = {
              cpu    = "0.2"
              memory = "256Mi"
            }
          }
          liveness_probe {
            http_get {
              path = "/health"
              port = 8080
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

variable "github_repo" {
  description = "GitHub repository name (org/repo format)"
  type        = string
  default     = "qkyrie/decentrifi"
}