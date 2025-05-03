resource "kubernetes_namespace" "decentrifi-namespace" {
  metadata {
    name = "decentrifi"
  }
}
