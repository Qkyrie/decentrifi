# Kubernetes Application Module

This Terraform module creates a standard Kubernetes deployment and service for decentrifi applications.

## Features

- Creates a Kubernetes deployment with configurable replicas
- Creates a Kubernetes service for the deployment
- Supports environment variables with secret references
- Configurable resource limits and requests
- Dynamic health probe configuration
- Service account integration
- Rolling update strategy

## Usage

```hcl
module "my_app" {
  source         = "./modules/kubernetes-app"
  app_name       = "my-app"
  namespace      = "my-namespace"
  github_repo    = "myorg/myrepo"
  container_port = 8080
  replicas       = 2
  
  env_vars = [
    {
      name  = "API_KEY"
      value = "some-value"
    },
    {
      name = "DB_PASSWORD"
      value_from = {
        secret_key_ref = {
          name = "my-secrets"
          key  = "db-password"
        }
      }
    }
  ]
  
  resources = {
    limits   = { cpu = "1", memory = "1Gi" }
    requests = { cpu = "0.5", memory = "512Mi" }
  }
  
  # Custom health probes
  liveness_probe = {
    http_get = {
      path = "/healthz"
      port = 8080
    }
    initial_delay_seconds = 60
    period_seconds        = 15
    failure_threshold     = 5
  }
  
  readiness_probe = {
    tcp_socket = {
      port = 8080
    }
    initial_delay_seconds = 10
    period_seconds        = 5
  }
}
```

## Health Probe Configuration

The module supports flexible health probe configuration for both liveness and readiness probes:

### HTTP Probes (Default)
```hcl
liveness_probe = {
  http_get = {
    path = "/health"
    port = 8080  # Optional, defaults to container_port
  }
  initial_delay_seconds = 30
  period_seconds        = 10
  timeout_seconds       = 5
  success_threshold     = 1
  failure_threshold     = 3
}
```

### TCP Socket Probes
```hcl
readiness_probe = {
  tcp_socket = {
    port = 8080  # Optional, defaults to container_port
  }
  initial_delay_seconds = 5
  period_seconds        = 5
}
```

### Default Probe Configuration

If no probe configuration is provided, the module uses sensible defaults:

- **Liveness Probe**: HTTP GET to `/health` on the container port
  - Initial delay: 30 seconds
  - Period: 10 seconds

- **Readiness Probe**: HTTP GET to `/health` on the container port
  - Initial delay: 10 seconds
  - Period: 5 seconds

### Disabling Probes

To disable a probe entirely, set it to `null`:

```hcl
liveness_probe  = null
readiness_probe = null
```

## Input Variables

| Name | Description | Type | Default |
|------|-------------|------|---------|
| app_name | Name of the application | string | - |
| namespace | Kubernetes namespace | string | - |
| github_repo | GitHub repository name (org/repo format) | string | - |
| container_port | Container port | number | 8080 |
| replicas | Number of replicas | number | 1 |
| resources | Resource requests & limits | object | See below |
| env_vars | Environment variables | list(object) | [] |
| liveness_probe | Liveness probe configuration | object | See below |
| readiness_probe | Readiness probe configuration | object | See below |

### Default Resources

```hcl
{
  limits   = { cpu = "0.5", memory = "512Mi" }
  requests = { cpu = "0.2", memory = "256Mi" }
}
```

### Default Probes

```hcl
liveness_probe = {
  http_get = {
    path = "/health"
    port = 8080
  }
  initial_delay_seconds = 30
  period_seconds        = 10
}

readiness_probe = {
  http_get = {
    path = "/health"
    port = 8080
  }
  initial_delay_seconds = 10
  period_seconds        = 5
}
```

## Outputs

| Name | Description |
|------|-------------|
| service_name | Name of the created Kubernetes service |
| deployment_name | Name of the created Kubernetes deployment |