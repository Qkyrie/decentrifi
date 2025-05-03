resource "kubernetes_ingress_v1" "decentrifi-ingress" {
  depends_on = [
    kubernetes_namespace.decentrifi-namespace,
    kubernetes_service.landing-page-html-service,
  ]
  wait_for_load_balancer = true
  metadata {
    name = "decentrifi-ingress"
    annotations = {
      "nginx.ingress.kubernetes.io/enable-cors"     = "true"
      "nginx.ingress.kubernetes.io/rewrite-target"  = "/$2"
      "nginx.ingress.kubernetes.io/proxy-body-size" = "20m"
      "nginx.ingress.kubernetes.io/port_in_redirect" = "off"
    }
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
  }
  spec {
    ingress_class_name = "nginx"
    tls {
      hosts = ["decentri.fi"]
      secret_name = "decentrifi.tls"
    }
    rule {
      host = "decentri.fi"
      http {
        path {
          path      = "/()(.*)"
          path_type = "Prefix"
          backend {
            service {
              name = "landing-page-html"
              port {
                number = 80
              }
            }
          }
        }
      }
    }
  }
}

# Separate ingress for www redirect
resource "kubernetes_ingress_v1" "decentrifi-www-redirect" {
  metadata {
    name = "www-redirect"
    annotations = {
      "nginx.ingress.kubernetes.io/permanent-redirect" = "https://decentri.fi$request_uri"
    }
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
  }
  spec {
    ingress_class_name = "nginx"
    rule {
      host = "www.decentri.fi"
      http {
        path {
          path = "/"
          path_type = "Prefix"
          backend {
            service {
              name = "landing-page-html"
              port {
                number = 80
              }
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_ingress_v1" "decentrifi-data-ingestion" {
  metadata {
    name = "data-ingestion-ingress"
    annotations = {
      "nginx.ingress.kubernetes.io/enable-cors"     = "true"
      "nginx.ingress.kubernetes.io/proxy-body-size" = "20m"
    }
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
  }
  spec {
    ingress_class_name = "nginx"
    tls {
      hosts = ["data.decentri.fi"]
      secret_name = "decentrifi.tls"
    }
    rule {
      host = "data.decentri.fi"
      http {
        path {
          path = "/"
          path_type = "Prefix"
          backend {
            service {
              name = "data-ingestion"
              port {
                number = 8080
              }
            }
          }
        }
      }
    }
  }
}
