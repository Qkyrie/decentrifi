resource "kubernetes_config_map" "landing-page-html" {
  metadata {
    name      = "landing-page-html"
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
  }
  data = {
    "index.html" = file("${path.module}/../landing/index.html")
  }
}

resource "kubernetes_deployment" "landing-page-html" {
  metadata {
    name      = "landing-page-html"
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
  }
  spec {
    replicas = 1
    selector {
      match_labels = {
        app = "landing-page-html"
      }
    }
    template {
      metadata {
        labels = {
          app = "landing-page-html"
        }
      }
      spec {
        container {
          name  = "landing-page"
          image = "nginx:alpine"
          volume_mount {
            mount_path = "/usr/share/nginx/html"
            name       = "config-volume"
          }
          port {
            container_port = 80
          }
        }
        volume {
          name = "config-volume"

          config_map {
            name = kubernetes_config_map.landing-page-html.metadata.0.name
          }
        }
      }
    }
  }
}


resource "kubernetes_service" "landing-page-html-service" {
  metadata {
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
    name      = "landing-page-html"
    labels = {
      team = "Decentrifi"
    }
  }

  spec {
    selector = {
      app = "landing-page-html"
    }

    port {
      name        = "http-traffic"
      port        = 80
      target_port = 80
      protocol    = "TCP"
    }
  }
}
