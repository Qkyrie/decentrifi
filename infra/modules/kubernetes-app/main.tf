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
          
          dynamic "liveness_probe" {
            for_each = var.liveness_probe != null ? [var.liveness_probe] : []
            content {
              dynamic "http_get" {
                for_each = liveness_probe.value.http_get != null ? [liveness_probe.value.http_get] : []
                content {
                  path = http_get.value.path
                  port = coalesce(http_get.value.port, var.container_port)
                }
              }
              dynamic "tcp_socket" {
                for_each = liveness_probe.value.tcp_socket != null ? [liveness_probe.value.tcp_socket] : []
                content {
                  port = coalesce(tcp_socket.value.port, var.container_port)
                }
              }
              initial_delay_seconds = liveness_probe.value.initial_delay_seconds
              period_seconds        = liveness_probe.value.period_seconds
              timeout_seconds       = liveness_probe.value.timeout_seconds
              success_threshold     = liveness_probe.value.success_threshold
              failure_threshold     = liveness_probe.value.failure_threshold
            }
          }
          
          dynamic "readiness_probe" {
            for_each = var.readiness_probe != null ? [var.readiness_probe] : []
            content {
              dynamic "http_get" {
                for_each = readiness_probe.value.http_get != null ? [readiness_probe.value.http_get] : []
                content {
                  path = http_get.value.path
                  port = coalesce(http_get.value.port, var.container_port)
                }
              }
              dynamic "tcp_socket" {
                for_each = readiness_probe.value.tcp_socket != null ? [readiness_probe.value.tcp_socket] : []
                content {
                  port = coalesce(tcp_socket.value.port, var.container_port)
                }
              }
              initial_delay_seconds = readiness_probe.value.initial_delay_seconds
              period_seconds        = readiness_probe.value.period_seconds
              timeout_seconds       = readiness_probe.value.timeout_seconds
              success_threshold     = readiness_probe.value.success_threshold
              failure_threshold     = readiness_probe.value.failure_threshold
            }
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