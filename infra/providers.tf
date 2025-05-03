terraform {
  required_providers {
    kubernetes = {
      source = "hashicorp/kubernetes",
    }
    cloudflare = {
      source  = "cloudflare/cloudflare"
      version = "~> 4.0"
    }
  }
}

variable "host" {
  type = string
  sensitive = true
}

variable "k8s_token" {
  type = string
  sensitive = true
}

variable "cloudflare_api_token" {
  type = string
  sensitive = true
}

variable "cluster_ca_certificate" {
  type = string
}

provider "kubernetes" {
  host = var.host

  token = var.k8s_token
  cluster_ca_certificate = base64decode(var.cluster_ca_certificate)
}

provider "cloudflare" {
  # Best practice: use an API token with scoped DNS edit rights
  api_token = var.cloudflare_api_token
}