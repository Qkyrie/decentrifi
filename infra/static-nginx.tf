# Deploy landing page using custom Docker image
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
          # Use image from container registry
          image = "ghcr.io/qkyrie/decentrifi/landing-page:latest"
          # Or use a specific version tag
          # image = "ghcr.io/${var.github_organization}/decentrifi/landing-page:${var.landing_page_version}"
          
          # Always pull latest image
          image_pull_policy = "Always"
          
          # Add GitHub container registry authentication
          port {
            container_port = 80
          }
        }
        
        # If using private repository, add image pull secret
        # image_pull_secrets {
        #   name = kubernetes_secret.gh-regcred.metadata.0.name
        # }
      }
    }
  }

  lifecycle {
    ignore_changes = [
      spec[0].template[0].metadata[0].annotations["kubectl.kubernetes.io/restartedAt"],
    ]
  }
}

# If using private repo, add this secret
# resource "kubernetes_secret" "gh-regcred" {
#   metadata {
#     namespace = kubernetes_namespace.decentrifi-namespace.metadata.0.name
#     name      = "gh-regcred"
#   }
#   
#   type = "kubernetes.io/dockerconfigjson"
#   
#   data = {
#     ".dockerconfigjson" = jsonencode({
#       auths = {
#         "ghcr.io" = {
#           auth = base64encode("${var.github_username}:${var.github_token}")
#         }
#       }
#     })
#   }
# }

# Add variables to main.tf
# variable "github_organization" {
#   description = "GitHub organization name"
#   type        = string
# }
# 
# variable "landing_page_version" {
#   description = "Landing page image version"
#   type        = string
#   default     = "latest"
# }

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
