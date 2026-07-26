# Renders the app VM's cloud-init. Managed DB endpoints are computed attributes
# (private IP + port), so referencing them here also establishes the
# create-order: databases first, then the VM whose cloud-init initialises them.

locals {
  pg_host    = try(vngcloud_vdb_relational_database.postgres.ip[0], "")
  pg_port    = vngcloud_vdb_relational_database.postgres.port
  redis_host = try(vngcloud_vdb_memstore_database.redis.ip[0], "")
  redis_port = vngcloud_vdb_memstore_database.redis.port

  # app.env — injected into the containers (env_file) AND sourced by
  # bootstrap.sh for the one-time schema load. No literal ${...}, so a plain
  # templatefile is safe.
  env_content = templatefile("${path.module}/templates/env.tftpl", {
    db_host = local.pg_host
    db_port = local.pg_port
    db_user = var.pg_username
    db_pass = var.pg_password
    db_name = var.pg_db_name

    redis_host = local.redis_host
    redis_port = local.redis_port
    redis_pass = var.redis_password

    keycloak_admin          = var.keycloak_admin
    keycloak_admin_password = var.keycloak_admin_password
    keycloak_realm          = var.keycloak_realm
    keycloak_client_id      = var.keycloak_client_id
    keycloak_client_secret  = var.keycloak_client_secret
    keycloak_version        = var.keycloak_version
    keycloak_command        = var.keycloak_command
    keycloak_hostname       = var.keycloak_hostname
    keycloak_callback_url   = var.keycloak_callback_url

    google_genai_api_key = var.google_genai_api_key

    app_repo_url = var.app_repo_url
    app_repo_ref = var.app_repo_ref
  })

  # These two are static (they contain literal ${...} shell/compose syntax that
  # must NOT be interpreted by Terraform), so read them verbatim with file().
  compose_content   = file("${path.module}/files/docker-compose.cloud.yml")
  bootstrap_content = file("${path.module}/files/bootstrap.sh")

  user_data = templatefile("${path.module}/templates/cloud-init.yaml.tftpl", {
    env_b64       = base64encode(local.env_content)
    compose_b64   = base64encode(local.compose_content)
    bootstrap_b64 = base64encode(local.bootstrap_content)
  })
}
