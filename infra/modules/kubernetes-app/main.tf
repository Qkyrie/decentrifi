resource "kubernetes_service" "app_service" {
  metadata {
    namespace   = var.namespace
    name        = var.app_name
    labels      = local.common_labels
    annotations = local.prometheus_annotations
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
    labels    = local.common_labels
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
        labels = local.common_labels
      }
      spec {
        service_account_name = "ingestion-trigger-sa"
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
              name  = env.value.name
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
            limits   = var.resources.limits
            requests = var.resources.requests
          }
          liveness_probe {
            http_get {
              path = "/health"
              port = var.container_port
            }
            initial_delay_seconds = 30
            period_seconds        = 10
          }
          readiness_probe {
            http_get {
              path = "/health"
              port = var.container_port
            }
            initial_delay_seconds = 10
            period_seconds        = 5
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