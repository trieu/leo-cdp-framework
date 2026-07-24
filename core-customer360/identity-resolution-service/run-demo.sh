#!/bin/bash
# End-to-end Customer Identity Resolution (CIR) demo:
#   1. Loads DB config from .env (see core-customer360/dev-start-pgsql.sh)
#   2. Creates/reuses a virtualenv and installs requirements.txt
#   3. Seeds sample AppsFlyer / MoEngage / Web Tracking raw profiles
#      (retail + banking domains)
#   4. Runs identity resolution batch(es) against them
#   5. Prints the resulting master profiles + ready-to-use psql commands so
#      you can inspect the data yourself.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE="${SCRIPT_DIR}/.env"
if [ -f "$ENV_FILE" ]; then
  echo "🔧 Loading database config from ${ENV_FILE}..."
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "⚠️  Warning: ${ENV_FILE} not found. Using default environment variables."
fi

VENV_DIR="${SCRIPT_DIR}/.venv"
if [ ! -d "$VENV_DIR" ]; then
  echo "📦 Creating virtual environment at ${VENV_DIR}..."
  python3 -m venv "$VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

echo "📥 Installing requirements..."
pip install -q -r requirements.txt

echo "🌱 Seeding sample AppsFlyer / MoEngage / Web Tracking data (retail + banking)..."
python scripts/init_sample_data.py

echo "⚙️  Running Customer Identity Resolution..."
python scripts/run_demo_resolution.py

echo "🌐 Seeding full demo data (CRM journey graph, relations, transactions, behavioral events, master-profile enrichment)..."
python scripts/seed_full_demo_data.py

DEMO_TENANT_ID="11111111-1111-1111-1111-111111111111"
PG_CONTAINER="${PG_CONTAINER:-pgsql16_vector}"
DB_NAME="${DB_NAME:-customer360}"
DB_SCHEMA="${DB_SCHEMA:-customer360}"

cat <<EOF

✅ Demo complete. Inspect the results yourself in psql, e.g.:

  docker exec -it -u postgres ${PG_CONTAINER} psql -d ${DB_NAME} -c \\
    "SELECT master_profile_id, domain, full_name, email, phone_number, source_systems FROM ${DB_SCHEMA}.cdp_master_profiles WHERE tenant_id = '${DEMO_TENANT_ID}' ORDER BY domain;"

  docker exec -it -u postgres ${PG_CONTAINER} psql -d ${DB_NAME} -c \\
    "SELECT raw_profile_id, source_system, domain, status_code FROM ${DB_SCHEMA}.cdp_raw_profiles_stage WHERE tenant_id = '${DEMO_TENANT_ID}' ORDER BY source_system;"

  docker exec -it -u postgres ${PG_CONTAINER} psql -d ${DB_NAME} -c \\
    "SELECT * FROM ${DB_SCHEMA}.cdp_profile_links WHERE tenant_id = '${DEMO_TENANT_ID}';"

  docker exec -it -u postgres ${PG_CONTAINER} psql -d ${DB_NAME} -c \\
    "SELECT lifecycle_stage, churn_risk_tier, clv_segment, engagement_score, preferred_channel FROM ${DB_SCHEMA}.cdp_master_profiles WHERE tenant_id = '${DEMO_TENANT_ID}' LIMIT 10;"

  docker exec -it -u postgres ${PG_CONTAINER} psql -d ${DB_NAME} -c \\
    "SELECT domain, event_category, event_name, COUNT(*) FROM ${DB_SCHEMA}.cdp_raw_events WHERE tenant_id = '${DEMO_TENANT_ID}' GROUP BY 1,2,3 ORDER BY 1,2,3;"

  docker exec -it -u postgres ${PG_CONTAINER} psql -d ${DB_NAME} -c \\
    "SELECT name, stage, value FROM ${DB_SCHEMA}.crm_opportunity ORDER BY value DESC;"

Or open an interactive shell:
  docker exec -it -u postgres ${PG_CONTAINER} psql -d ${DB_NAME}
EOF
