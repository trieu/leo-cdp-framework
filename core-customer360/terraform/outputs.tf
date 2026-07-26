output "app_vm_id" {
  description = "ID of the app VM."
  value       = vngcloud_vserver_server.app.id
}

output "app_vm_interfaces" {
  description = "App VM network interfaces (includes the floating/public IP). Read floating_ip from here after apply."
  value       = vngcloud_vserver_server.app.external_interfaces
}

output "postgres_host" {
  description = "Private endpoint of the managed PostgreSQL instance."
  value       = try(vngcloud_vdb_relational_database.postgres.ip[0], null)
}

output "postgres_port" {
  value = vngcloud_vdb_relational_database.postgres.port
}

output "postgres_database" {
  value = var.pg_db_name
}

output "redis_host" {
  description = "Private endpoint of the managed Redis instance."
  value       = try(vngcloud_vdb_memstore_database.redis.ip[0], null)
}

output "redis_port" {
  value = vngcloud_vdb_memstore_database.redis.port
}

output "next_steps" {
  description = "What to do after apply."
  value       = <<-EOT
    1. Find the floating IP in `app_vm_interfaces` (or the console).
    2. cloud-init takes a few minutes (Docker install + repo build + schema load).
       Watch it:  ssh ubuntu@<floating_ip> 'tail -f /var/log/cloud-init-output.log'
    3. API:      http://<floating_ip>:8000/health
       Keycloak: http://<floating_ip>:8080
  EOT
}
