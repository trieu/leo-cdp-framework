#!/bin/bash

AIRFLOW_VERSION="2.11.0"

set -euo pipefail

# === Load environment variables from .env if present ===
if [ -f .env ]; then
  echo "📥 Loading environment variables from .env"
  # Export all non-comment lines
  export $(grep -v '^#' .env | xargs)
fi

# 🚫 Disable sample/example DAGs
export AIRFLOW__CORE__LOAD_EXAMPLES=False

# === Core configuration ===
export AIRFLOW_HOME="$(pwd)"
VENV_DIR="$AIRFLOW_HOME/airflow-venv"
export AIRFLOW__CORE__DAGS_FOLDER="$AIRFLOW_HOME/airflow-dags"
export AIRFLOW__API__AUTH_BACKENDS="airflow.api.auth.backend.basic_auth"


OUTPUT_DIR="$AIRFLOW_HOME/airflow-output"
mkdir -p "$OUTPUT_DIR"

DATE=$(date +%Y-%m-%d)
WEBSERVER_LOG="$OUTPUT_DIR/webserver-$DATE.log"
SCHEDULER_LOG="$OUTPUT_DIR/scheduler-$DATE.log"

# Use defaults if not in .env
AIRFLOW_WEB_SERVER_HOST="${AIRFLOW_WEB_SERVER_HOST:-0.0.0.0}"
AIRFLOW_WEB_SERVER_PORT="${AIRFLOW_WEB_SERVER_PORT:-8080}"
AIRFLOW_USERNAME="${AIRFLOW_USERNAME:-admin}"
AIRFLOW_PASSWORD="${AIRFLOW_PASSWORD:-leocdp123}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@example.com}"
DEV_MODE="${DEV_MODE:-true}"

# === Virtualenv activation ===
echo "📥 Activating venv and using Airflow $AIRFLOW_VERSION"
source "$VENV_DIR/bin/activate"

# === Initialize DB ===
airflow db upgrade >> "$OUTPUT_DIR/db-upgrade-$DATE.log" 2>&1

# === Create default admin user if DEV_MODE is true ===
if [ "$DEV_MODE" = true ]; then
  if ! airflow users list | grep -q "$AIRFLOW_USERNAME"; then
    echo "DEV_MODE → Creating admin user (username=$AIRFLOW_USERNAME)"
    airflow users create \
      --username "$AIRFLOW_USERNAME" \
      --firstname Airflow \
      --lastname Admin \
      --role Admin \
      --email "$ADMIN_EMAIL" \
      --password "$AIRFLOW_PASSWORD"
  else
    echo "DEV_MODE → Admin user '$AIRFLOW_USERNAME' already exists."
  fi
else
  echo "DEV_MODE disabled → Skipping default admin user creation."
fi

# === Start webserver ===
echo "🚀 Starting Airflow Webserver (logs: $WEBSERVER_LOG)..."
airflow webserver --host "$AIRFLOW_WEB_SERVER_HOST" --port "$AIRFLOW_WEB_SERVER_PORT" >> "$WEBSERVER_LOG" 2>&1 &

# === Start scheduler ===
echo "🌀 Starting Airflow Scheduler (logs: $SCHEDULER_LOG)..."
airflow scheduler >> "$SCHEDULER_LOG" 2>&1 &

# === Summary ===
echo "✅ Airflow $AIRFLOW_VERSION is running"
echo "   AIRFLOW_HOME=$AIRFLOW_HOME"
echo "   DAGS_FOLDER=$AIRFLOW__CORE__DAGS_FOLDER"
echo "   Logs in $OUTPUT_DIR"
echo "   Login at http://$AIRFLOW_WEB_SERVER_HOST:$AIRFLOW_WEB_SERVER_PORT"
echo "   → Username: $AIRFLOW_USERNAME"
echo "   → Password: $AIRFLOW_PASSWORD"