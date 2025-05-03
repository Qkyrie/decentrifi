terraform {
  required_providers {
    kubernetes = {
      source = "hashicorp/kubernetes",
    }
  }
}

variable "host" {
  type = string
}

variable "k8s_token" {
  type = string
}

variable "cluster_ca_certificate" {
  type = string
}

provider "kubernetes" {
  host = var.host

  token = var.k8s_token
  cluster_ca_certificate = base64decode(var.cluster_ca_certificate)
}