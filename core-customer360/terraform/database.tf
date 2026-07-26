# Managed databases (VNG Cloud vDB), replacing the postgres + redis containers
# from docker-compose.yml.
#
# IMPORTANT: the compose stack uses a CUSTOM Postgres image (postgis/postgis:16
# + pgvector). A managed RDS instance is a stock PostgreSQL. cloud-init runs
# `CREATE EXTENSION postgis/vector/...` against it (postgres/init/00-extensions.sql)
# — this only works if your vDB plan permits those extensions. If it does not,
# fall back to self-hosting Postgres on a VM with the repo's custom image.
# See README.md > Caveats.

# --- Managed PostgreSQL (RDS) ------------------------------------------------
data "vngcloud_vdb_database_package" "postgres" {
  engine_type    = "PostgreSQL"
  engine_version = var.pg_engine_version
  name           = var.pg_package_name
  zone_id        = var.zone_id
}

data "vngcloud_vdb_database_volume_type" "postgres" {
  type    = var.pg_volume_type
  zone_id = var.zone_id
}

resource "vngcloud_vdb_relational_database" "postgres" {
  name           = "${var.name_prefix}-pg"
  engine_type    = "PostgreSQL"
  engine_version = var.pg_engine_version
  subnet_id      = vngcloud_vserver_subnet.main.id

  package_id  = data.vngcloud_vdb_database_package.postgres.id
  volume_type = data.vngcloud_vdb_database_volume_type.postgres.id
  volume_size = var.pg_volume_size

  db_name  = var.pg_db_name
  username = var.pg_username
  password = var.pg_password

  # Private to the VPC: reachable only from the app subnet.
  public_access     = false
  allowed_ip_prefix = [var.subnet_cidr]

  backup_auto     = true
  backup_duration = var.db_backup_retention_days
  backup_time     = var.db_backup_time

  zone_id = var.zone_id
}

# --- Managed Redis (MemoryStore) ---------------------------------------------
data "vngcloud_vdb_database_package" "redis" {
  engine_type    = "Redis"
  engine_version = var.redis_engine_version
  name           = var.redis_package_name
  zone_id        = var.zone_id
}

resource "vngcloud_vdb_memstore_database" "redis" {
  name           = "${var.name_prefix}-redis"
  engine_type    = "Redis"
  engine_version = var.redis_engine_version
  subnet_id      = vngcloud_vserver_subnet.main.id

  package_id = data.vngcloud_vdb_database_package.redis.id

  redis_password_enabled = true
  redis_password         = var.redis_password

  public_access     = false
  allowed_ip_prefix = [var.subnet_cidr]

  backup_auto     = true
  backup_duration = var.db_backup_retention_days
  backup_time     = var.db_backup_time

  zone_id = var.zone_id
}
