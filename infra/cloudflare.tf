data "cloudflare_zone" "main" {
  name = var.hostname_domain  # e.g. "example.com"
}

variable "hostname_domain" {
  description = "Your root domain (e.g. example.com)"
  type        = string
}

variable "cluster_ip" {
  description = "Cluster IP"
  type        = string
}

resource "cloudflare_record" "app_root" {
  zone_id = data.cloudflare_zone.main.id
  name    = "@"
  type    = "A"
  content   = var.cluster_ip
  ttl     = 1                  # in seconds; use 1 for “automatic”
  proxied = true                  # set to false if you don’t want Cloudflare proxy
}

resource "cloudflare_record" "app_www" {
  zone_id = data.cloudflare_zone.main.id
  name = "www"
  type = "CNAME"
  content = "@"
  ttl = 1
  proxied = true
}

resource "cloudflare_record" "app_data" {
  zone_id = data.cloudflare_zone.main.id
  name = "data"
  type = "CNAME"
  content = "@"
  ttl = 1
  proxied = true
}
