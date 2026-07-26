#!/usr/bin/env bash
# =============================================================================
# App VM bootstrap (run once by cloud-init).
#   1. install Docker Engine + compose plugin
#   2. clone/checkout the monorepo
#   3. initialise the managed PostgreSQL: extensions + db_keycloak + schema
#   4. bring up api/cir/keycloak via docker compose
# Idempotent: safe to re-run (e.g. `sudo bash /opt/customer360/bootstrap.sh`).
# =============================================================================
set -euo pipefail

APP_DIR=/opt/customer360
ENV_FILE="$APP_DIR/app.env"
SRC_DIR="$APP_DIR/src"

log() { echo "[bootstrap] $(date -u +%FT%TZ) $*"; }

# Load DB_*, APP_REPO_* etc. into the environment.
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

# --- 1. Docker ---------------------------------------------------------------
if ! command -v docker >/dev/null 2>&1; then
  log "Installing Docker Engine..."
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker

# --- 2. Application source ---------------------------------------------------
if [ ! -d "$SRC_DIR/.git" ]; then
  log "Cloning $APP_REPO_URL ..."
  git clone "$APP_REPO_URL" "$SRC_DIR"
fi
log "Checking out $APP_REPO_REF ..."
git -C "$SRC_DIR" fetch --all --tags --prune
git -C "$SRC_DIR" checkout "$APP_REPO_REF"
git -C "$SRC_DIR" pull --ff-only || true

CORE="$SRC_DIR/core-customer360"
cp "$ENV_FILE" "$CORE/.env"
cp "$APP_DIR/docker-compose.cloud.yml" "$CORE/docker-compose.cloud.yml"

# --- 3. Managed PostgreSQL init ---------------------------------------------
export PGPASSWORD="$DB_PASSWORD"
PSQL=(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -v ON_ERROR_STOP=1)

log "Waiting for PostgreSQL at $DB_HOST:$DB_PORT ..."
for _ in $(seq 1 60); do
  if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; then
    break
  fi
  sleep 5
done

# Extensions required by database-schema.sql (postgis, vector, uuid-ossp, ...).
# If your managed plan forbids CREATE EXTENSION, this step fails loudly — see
# the README caveat about PostGIS/pgvector on managed RDS.
log "Ensuring PostgreSQL extensions..."
"${PSQL[@]}" -d "$DB_NAME" -f "$CORE/postgres/init/00-extensions.sql"

log "Ensuring db_keycloak database..."
"${PSQL[@]}" -d "$DB_NAME" -f "$CORE/postgres/init/02-create-keycloak-db.sql"

# Load the Customer360 schema only if it hasn't been loaded yet (idempotent
# across VM reprovisions — the managed DB is the source of truth).
TABLES="$("${PSQL[@]}" -d "$DB_NAME" -tAc \
  "SELECT count(*) FROM information_schema.tables WHERE table_schema='customer360'")"
if [ "${TABLES:-0}" = "0" ]; then
  log "Loading customer360 schema..."
  "${PSQL[@]}" -d "$DB_NAME" -f "$CORE/database-schema.sql"
else
  log "customer360 schema already present ($TABLES tables) — skipping load."
fi

# --- 4. Start the stateless services ----------------------------------------
log "Starting api/cir/keycloak..."
cd "$CORE"
docker compose -f docker-compose.cloud.yml up -d --build

log "Bootstrap complete."
