
#!/bin/bash
# =============================================================================
# Customer 360 Platform - local DEV bootstrap (infra-only)
#
# Starts the infra-only stack in dev-docker-compose.yml (postgres + redis +
# keycloak) so customer360-api and identity-resolution-service (CIR) can be
# run directly on the host against dockerized Postgres/Redis -- see
# customer360-api/start.sh and identity-resolution-service/run-demo.sh, and
# "non-Docker local dev workflow" in DOCKER-COMPOSE-GUIDE.md section 10.
#
# What it does, in order:
#   1. Ensures '.env' exists (created from '.env.example' if missing) and
#      contains every key currently in '.env.example'.
#   2. Starts (or resets) postgres/redis/keycloak via
#      `docker compose -f dev-docker-compose.yml`.
#   3. Waits for all three containers to report healthy.
#   4. Checks whether the Keycloak 'leocdp' realm exists yet; there is no
#      automated realm/client seed script in this repo, so it prints manual
#      setup instructions (DOCKER-COMPOSE-GUIDE.md section 9) when missing.
#   5. Checks whether customer360.cdp_master_profiles is empty and, if so,
#      seeds CIR demo data via identity-resolution-service/run-demo.sh.
#
# Usage:
#   ./dev-start-all.sh              Start/create services, sync .env, seed
#                                    CIR demo data if the DB looks empty.
#   ./dev-start-all.sh --no-seed    Same, but skip the CIR demo data seed step.
#   ./dev-start-all.sh reset        DESTRUCTIVE: `docker compose down -v`
#                                    (drops the postgres/redis volumes -- this
#                                    also wipes Keycloak's db_keycloak) then
#                                    starts fresh and reseeds.
#   ./dev-start-all.sh reset -y     Same as 'reset' but skips the confirmation
#                                    prompt (CI / automation).
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="dev-docker-compose.yml"
ENV_FILE=".env"
ENV_EXAMPLE_FILE=".env.example"
CIR_DIR="identity-resolution-service"
POSTGRES_CONTAINER="customer360-postgres"
REDIS_CONTAINER="customer360-redis"
KEYCLOAK_CONTAINER="customer360-keycloak"

# --- Parse args (order-independent) ---
ACTION="up"
SKIP_CONFIRM="false"
SKIP_SEED="false"
for arg in "$@"; do
  case "$arg" in
    reset) ACTION="reset" ;;
    -y|--yes) SKIP_CONFIRM="true" ;;
    --no-seed) SKIP_SEED="true" ;;
    -h|--help)
      sed -n '2,29p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *)
      echo "❌ Unknown argument: $arg (use -h for usage)" >&2
      exit 1
      ;;
  esac
done

# --- docker compose v2 required (depends_on: condition: service_healthy) ---
if docker compose version >/dev/null 2>&1; then
  DC=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  echo "⚠️  Warning: falling back to legacy 'docker-compose' v1 -- 'depends_on: condition: service_healthy' requires Compose v2 (the 'docker compose' plugin)." >&2
  DC=(docker-compose)
else
  echo "❌ Error: neither 'docker compose' (v2 plugin) nor 'docker-compose' found on PATH." >&2
  exit 1
fi
DC_CMD=("${DC[@]}" -f "$COMPOSE_FILE")

# =============================================================================
# 1) .env bootstrap: create from .env.example if missing, then add any keys
#    present in .env.example but missing from .env (without touching values
#    the user already customized).
# =============================================================================
ensure_env_file() {
  if [ ! -f "$ENV_FILE" ]; then
    if [ ! -f "$ENV_EXAMPLE_FILE" ]; then
      echo "❌ Error: neither '${ENV_FILE}' nor '${ENV_EXAMPLE_FILE}' found in ${SCRIPT_DIR}." >&2
      exit 1
    fi
    echo "📄 '${ENV_FILE}' not found -- creating it from '${ENV_EXAMPLE_FILE}'..."
    cp "$ENV_EXAMPLE_FILE" "$ENV_FILE"
    echo "⚠️  Edit '${ENV_FILE}' and set real values for DB_PASSWORD, REDIS_PASSWORD, KEYCLOAK_ADMIN_PASSWORD (and KEYCLOAK_CLIENT_SECRET once the client exists -- see DOCKER-COMPOSE-GUIDE.md section 9)."
  fi
}

sync_env_keys() {
  local added=0
  local key line
  while IFS= read -r line || [ -n "$line" ]; do
    [[ "$line" =~ ^[[:space:]]*# ]] && continue
    [[ "$line" != *=* ]] && continue
    key="${line%%=*}"
    [ -z "$key" ] && continue
    if ! grep -qE "^${key}=" "$ENV_FILE"; then
      if [ "$added" -eq 0 ]; then
        {
          echo ""
          echo "# --- Added by dev-start-all.sh on $(date +%Y-%m-%d) from ${ENV_EXAMPLE_FILE} ---"
        } >> "$ENV_FILE"
      fi
      echo "$line" >> "$ENV_FILE"
      echo "➕ Added missing key '${key}' to '${ENV_FILE}' (review its value)."
      added=$((added + 1))
    fi
  done < "$ENV_EXAMPLE_FILE"
  if [ "$added" -gt 0 ]; then
    echo "⚠️  ${added} new key(s) added to '${ENV_FILE}' with default/placeholder values -- review before relying on them."
  fi
}

echo "🔧 Checking '${ENV_FILE}'..."
ensure_env_file
sync_env_keys
# shellcheck disable=SC1091
set -a
source "$ENV_FILE"
set +a

# DB_PORT/REDIS_PORT are what host-run apps (customer360-api/start.sh,
# identity-resolution-service/run-demo.sh) connect through; *_HOST_PORT is
# what docker-compose publishes. They must match when running against the
# dockerized services from the host.
if [ "${DB_PORT:-5432}" != "${POSTGRES_HOST_PORT:-5432}" ]; then
  echo "⚠️  DB_PORT (${DB_PORT:-5432}) != POSTGRES_HOST_PORT (${POSTGRES_HOST_PORT:-5432}) in '${ENV_FILE}' -- host-run apps connecting via DB_PORT may not reach the published Postgres port."
fi
if [ "${REDIS_PORT:-6379}" != "${REDIS_HOST_PORT:-6379}" ]; then
  echo "⚠️  REDIS_PORT (${REDIS_PORT:-6379}) != REDIS_HOST_PORT (${REDIS_HOST_PORT:-6379}) in '${ENV_FILE}' -- host-run apps connecting via REDIS_PORT may not reach the published Redis port."
fi

# =============================================================================
# 2) Start / reset postgres + redis + keycloak
# =============================================================================
if [ "$ACTION" = "reset" ]; then
  echo "⚠️  This will run '${DC[*]} -f ${COMPOSE_FILE} down -v', PERMANENTLY DELETING the customer360-pgdata and customer360-redisdata volumes (all Postgres + Redis data, including Keycloak's db_keycloak)."
  if [ "$SKIP_CONFIRM" != "true" ]; then
    read -r -p "Type 'yes' to confirm: " CONFIRM_ANSWER
    if [ "$CONFIRM_ANSWER" != "yes" ]; then
      echo "❌ Aborted. No changes made."
      exit 1
    fi
  fi
  echo "🗑️  Tearing down existing containers + volumes..."
  "${DC_CMD[@]}" down -v
fi

echo "🚀 Starting postgres + redis + keycloak (${COMPOSE_FILE})..."
"${DC_CMD[@]}" up -d --build

# =============================================================================
# 3) Wait for all three services to report healthy
# =============================================================================
wait_for_healthy() {
  local container="$1"
  local max_attempts=30
  local attempt=1
  echo "⏳ Waiting for '${container}' to become healthy..."
  until [ "$(docker inspect -f '{{.State.Health.Status}}' "$container" 2>/dev/null)" = "healthy" ]; do
    if [ "$attempt" -ge "$max_attempts" ]; then
      echo "❌ Error: '${container}' did not become healthy after ${max_attempts} attempts." >&2
      "${DC_CMD[@]}" logs --tail=50 "$container" || true
      exit 1
    fi
    sleep 2
    attempt=$((attempt + 1))
  done
  echo "🟢 '${container}' is healthy."
}

wait_for_healthy "$POSTGRES_CONTAINER"
wait_for_healthy "$REDIS_CONTAINER"
wait_for_healthy "$KEYCLOAK_CONTAINER"

# =============================================================================
# 4) Keycloak realm check -- no automated realm/client seed script exists in
#    this repo (see DOCKER-COMPOSE-GUIDE.md section 9), so just detect and
#    point at the manual steps instead of pretending to seed it.
# =============================================================================
check_keycloak_realm() {
  local realm="${KEYCLOAK_REALM:-leocdp}"
  echo "🔎 Checking whether Keycloak realm '${realm}' exists..."
  local exists
  exists="$(docker exec -u postgres "$POSTGRES_CONTAINER" psql -U "${DB_USER:-postgres}" -d db_keycloak -tAc \
    "SELECT 1 FROM realm WHERE name = '${realm}'" 2>/dev/null || true)"
  if [ "$exists" != "1" ]; then
    cat <<EOF
⚠️  Keycloak realm '${realm}' not found in 'db_keycloak'. There is no
   automated realm/client seed script in this repo -- create it manually:
     1. Open http://localhost:${KEYCLOAK_HOST_PORT:-8080} and log in with
        KEYCLOAK_ADMIN / KEYCLOAK_ADMIN_PASSWORD from '${ENV_FILE}'.
     2. Follow DOCKER-COMPOSE-GUIDE.md section 9 to create the '${realm}'
        realm, the '${KEYCLOAK_CLIENT_ID:-leocdp}' confidential client, and a
        test user, then copy the client secret into KEYCLOAK_CLIENT_SECRET.
EOF
  else
    echo "🟢 Keycloak realm '${realm}' already exists."
  fi
}
check_keycloak_realm

# =============================================================================
# 5) Seed identity-resolution (CIR) demo data if the DB looks empty
# =============================================================================
seed_cir_if_empty() {
  local db_name="${DB_NAME:-customer360}"
  local db_schema="${DB_SCHEMA:-customer360}"
  echo "🔎 Checking whether '${db_schema}.cdp_master_profiles' has any data..."
  local count
  count="$(docker exec -u postgres "$POSTGRES_CONTAINER" psql -U "${DB_USER:-postgres}" -d "$db_name" -tAc \
    "SELECT COUNT(*) FROM ${db_schema}.cdp_master_profiles" 2>/dev/null || true)"
  if [ -z "$count" ]; then
    echo "⚠️  Could not query '${db_schema}.cdp_master_profiles' (schema not applied yet?) -- skipping CIR seed." >&2
    return
  fi
  if [ "$count" -eq 0 ]; then
    if [ ! -f "${CIR_DIR}/run-demo.sh" ]; then
      echo "⚠️  '${CIR_DIR}/run-demo.sh' not found -- skipping CIR seed." >&2
      return
    fi
    echo "🌱 '${db_schema}.cdp_master_profiles' is empty -- seeding CIR demo data via ${CIR_DIR}/run-demo.sh..."
    (cd "$CIR_DIR" && bash run-demo.sh)
  else
    echo "🟢 '${db_schema}.cdp_master_profiles' already has ${count} row(s) -- skipping CIR seed."
  fi
}

if [ "$SKIP_SEED" = "true" ]; then
  echo "⏭️  --no-seed set -- skipping CIR demo data seed check."
else
  seed_cir_if_empty
fi

echo "✅ Done. postgres (:${POSTGRES_HOST_PORT:-5432}) + redis (:${REDIS_HOST_PORT:-6379}) + keycloak (:${KEYCLOAK_HOST_PORT:-8080}) are up."