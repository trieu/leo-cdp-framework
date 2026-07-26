# =============================================================================
# Input variables
#
# Required (no default): vng_client_id, vng_client_secret, project_id,
# ssh_public_key, pg_password, redis_password, keycloak_admin_password,
# keycloak_client_secret, app_repo_url.
#
# Everything with a default is a sensible starting point but MUST be checked
# against your GreenNode/VNG project catalog (flavor/image/package/volume-type
# NAMES differ per region and change over time). See README.md.
# =============================================================================

# ----- Provider / account ----------------------------------------------------
variable "vng_client_id" {
  type        = string
  description = "VNG Cloud API client_id (IAM / API key)."
  sensitive   = true
}

variable "vng_client_secret" {
  type        = string
  description = "VNG Cloud API client_secret (IAM / API key)."
  sensitive   = true
}

variable "vng_token_url" {
  type        = string
  description = "IAM token endpoint. Override if your account/region differs."
  default     = "https://iamapis.vngcloud.vn/accounts-api/v2/auth/token"
}

variable "vng_vserver_base_url" {
  type        = string
  description = "vServer gateway endpoint (region-specific)."
  default     = "https://hcm-3.api.vngcloud.vn/vserver/vserver-gateway"
}

variable "vng_vlb_base_url" {
  type        = string
  description = "vLB gateway endpoint (region-specific)."
  default     = "https://hcm-3.api.vngcloud.vn/vserver/vlb-gateway"
}

variable "vng_vdb_base_url" {
  type        = string
  description = "vDB (managed database) gateway endpoint."
  default     = "https://vdb-gateway.vngcloud.vn"
}

variable "project_id" {
  type        = string
  description = "VNG Cloud project ID (looks like pro-xxxxxxxx-...)."
}

variable "zone_id" {
  type        = string
  description = "Availability zone. Allowed: HCM03-1A, HCM03-1B, HCM03-1C."
  default     = "HCM03-1A"
}

variable "name_prefix" {
  type        = string
  description = "Prefix for all created resource names."
  default     = "customer360"
}

# ----- Networking ------------------------------------------------------------
variable "network_cidr" {
  type        = string
  description = "VPC network CIDR (/16). Allowed ranges per VNG: 10.0.0.0-10.255.0.0, 172.16.0.0-172.24.0.0, 192.168.0.0."
  default     = "10.76.0.0/16"
}

variable "subnet_cidr" {
  type        = string
  description = "Subnet CIDR (must sit inside network_cidr). Also used as the allowed source prefix for the managed databases."
  default     = "10.76.1.0/24"
}

variable "allowed_ssh_cidr" {
  type        = string
  description = "Source CIDR permitted to SSH (22) to the app VM. RESTRICT THIS to your office/VPN IP in production."
  default     = "0.0.0.0/0"
}

variable "allowed_api_cidr" {
  type        = string
  description = "Source CIDR permitted to reach the API (8000)."
  default     = "0.0.0.0/0"
}

variable "allowed_keycloak_cidr" {
  type        = string
  description = "Source CIDR permitted to reach Keycloak (8080)."
  default     = "0.0.0.0/0"
}

# ----- App VM (vServer) ------------------------------------------------------
variable "ssh_public_key" {
  type        = string
  description = "SSH public key material (ssh-rsa/ssh-ed25519 ...) for the app VM."
}

variable "flavor_zone_name" {
  type        = string
  description = "vServer flavor-zone display name (e.g. 'General v1 Instances'). VERIFY in the console."
  default     = "General v1 Instances"
}

variable "vm_flavor_name" {
  type        = string
  description = "vServer flavor name. The stack (api+cir+keycloak+builds) wants >= 2 vCPU / 4 GB. VERIFY/ADJUST — the default is small."
  default     = "dev.v1.small1x1"
}

variable "vm_image_name" {
  type        = string
  description = "OS image name — must match EXACTLY what your project lists (e.g. 'Ubuntu 22.04'). cloud-init assumes an apt/systemd Ubuntu/Debian image."
  default     = "Ubuntu 22.04"
}

variable "volume_type_zone_name" {
  type        = string
  description = "Block-store volume-type-zone display name (e.g. 'SSD')."
  default     = "SSD"
}

variable "root_volume_type_name" {
  type        = string
  description = "Root disk volume type name (e.g. 'SSD-IOPS3000')."
  default     = "SSD-IOPS3000"
}

variable "vm_root_disk_size" {
  type        = number
  description = "App VM root disk size (GB)."
  default     = 40
}

# ----- Managed PostgreSQL (vDB / RDS) ----------------------------------------
variable "pg_engine_version" {
  type        = string
  description = "PostgreSQL engine version. Compose uses 16. Confirm availability in your project."
  default     = "16"
}

variable "pg_package_name" {
  type        = string
  description = "vDB package name for the RDS instance (e.g. 'db.s-general-1x2'). VERIFY."
  default     = "db.s-general-1x2"
}

variable "pg_volume_type" {
  type        = string
  description = "vDB volume type for RDS (e.g. 'Gen2-NVMe2-IOPS3000'). VERIFY."
  default     = "Gen2-NVMe2-IOPS3000"
}

variable "pg_volume_size" {
  type        = number
  description = "RDS data volume size (GB)."
  default     = 20
}

variable "pg_db_name" {
  type        = string
  description = "Application database name (maps to DB_NAME in the app)."
  default     = "customer360"
}

variable "pg_username" {
  type        = string
  description = "RDS master username (maps to DB_USER)."
  default     = "customer360"
}

variable "pg_password" {
  type        = string
  description = "RDS master password (maps to DB_PASSWORD). Mind the provider's complexity rules."
  sensitive   = true
}

# ----- Managed Redis (vDB / MemoryStore) -------------------------------------
variable "redis_engine_version" {
  type        = string
  description = "Redis engine version. Confirm which versions your project offers."
  default     = "7.0"
}

variable "redis_package_name" {
  type        = string
  description = "vDB package name for the Redis (MemoryStore) instance. VERIFY."
  default     = "db.s-general-1x2"
}

variable "redis_password" {
  type        = string
  description = "Redis AUTH password (maps to REDIS_PASSWORD). VNG typically requires >= 16 chars."
  sensitive   = true
}

# ----- Managed DB backups (shared by both instances) -------------------------
variable "db_backup_retention_days" {
  type        = number
  description = "Automatic backup retention in days (min 2, max 14)."
  default     = 7
  validation {
    condition     = var.db_backup_retention_days >= 2 && var.db_backup_retention_days <= 14
    error_message = "db_backup_retention_days must be between 2 and 14."
  }
}

variable "db_backup_time" {
  type        = string
  description = "Daily backup time, 'HH:MM'."
  default     = "18:00"
}

# ----- Keycloak / app config -------------------------------------------------
variable "keycloak_admin" {
  type        = string
  description = "Keycloak bootstrap admin username."
  default     = "admin"
}

variable "keycloak_admin_password" {
  type        = string
  description = "Keycloak bootstrap admin password."
  sensitive   = true
}

variable "keycloak_realm" {
  type    = string
  default = "leocdp"
}

variable "keycloak_client_id" {
  type    = string
  default = "leocdp"
}

variable "keycloak_client_secret" {
  type        = string
  description = "OIDC client secret the API uses to talk to Keycloak."
  sensitive   = true
}

variable "keycloak_version" {
  type        = string
  description = "Keycloak container image tag (matches docker-compose default)."
  default     = "26.7"
}

variable "keycloak_command" {
  type        = string
  description = "'start-dev' (default, easiest) or 'start' for a hardened prod run behind TLS. See README for prod hostname/proxy notes."
  default     = "start-dev"
}

variable "keycloak_hostname" {
  type        = string
  description = "Public hostname/IP Keycloak advertises in tokens/URLs. Set to the VM floating IP or a real domain for anything beyond local testing."
  default     = "localhost"
}

variable "keycloak_callback_url" {
  type    = string
  default = "http://localhost:8000/auth/callback"
}

variable "google_genai_api_key" {
  type        = string
  description = "Optional Google GenAI API key (used by the CIR/demo tooling)."
  default     = ""
  sensitive   = true
}

# ----- Application source (built on the VM by cloud-init) --------------------
variable "app_repo_url" {
  type        = string
  description = "Git URL of the monorepo. Must be reachable from the VM (public, or embed a token / use a deploy key for private repos). cloud-init builds customer360-api and identity-resolution-service from core-customer360/."
}

variable "app_repo_ref" {
  type        = string
  description = "Git branch/tag/commit to check out."
  default     = "main"
}
