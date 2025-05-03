resource "kubernetes_service" "data-ingestion-service" {
  metadata {
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
    name      = "data-ingestion"
    labels = {
      team = "decentrifi"
    }
    annotations = {
      "prometheus.io/scrape" : "true"
      "prometheus.io/port" : "8080",
      "prometheus.io/path" : "/actuator/prometheus"
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

resource "kubernetes_service" "data-ingestion-service" {
  metadata {
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
    name      = "cipheredge"
    labels = {
      team = "cipheredge"
    }
    annotations = {
      "prometheus.io/scrape" : "true"
      "prometheus.io/port" : "8080",
      "prometheus.io/path" : "/actuator/prometheus"
    }
  }

  spec {
    selector = {
      app = "cipheredge"
    }

    port {
      name        = "http-traffic"
      port        = 8080
      target_port = 8080
      protocol    = "TCP"
    }
  }
}


resource "kubernetes_deployment" "ingestion-service-deployment" {
  metadata {
    namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
    name      = "ingestion-service"
    labels = {
      app : "ingestion-service"
    }
  }
  spec {
    replicas = "1"
    selector {
      match_labels = {
        app : "ingestion-service"
      }
    }
    strategy {
      type = "RollingUpdate"
    }
    template {
      metadata {
        labels = {
          app : "ingestion-service"
        }
      }
      spec {
        volume {
          name = "config-volume"
          config_map {
            name = "cipheredge"
          }
        }
        container {
          image             = "ghcr.io/qkyrie/cipheredge:latest"
          name              = "cipheredge"
          image_pull_policy = "Always"
          port {
            container_port = 8080
          }
        }
        toleration {
          key      = "node-role.kubernetes.io/master"
          effect   = "NoSchedule"
          operator = "Exists"
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

