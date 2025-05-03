output "service_name" {
  description = "The name of the Kubernetes service"
  value       = kubernetes_service.app_service.metadata[0].name
}

output "deployment_name" {
  description = "The name of the Kubernetes deployment"
  value       = kubernetes_deployment.app_deployment.metadata[0].name
}

output "service_resource" {
  description = "The Kubernetes service resource"
  value       = kubernetes_service.app_service
}

output "deployment_resource" {
  description = "The Kubernetes deployment resource"
  value       = kubernetes_deployment.app_deployment
}