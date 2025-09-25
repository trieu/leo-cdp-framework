#!/bin/bash

AIRFLOW_VERSION="2.11.0"
DEV_MODE=true

# Admin user credentials
ADMIN_EMAIL=admin@example.com
DEFAULT_ADMIN_PASSWORD=leocdp123

set -euo pipefail

# Use current folder as AIRFLOW_HOME
export AIRFLOW_HOME="$(pwd)"
VENV_DIR="$AIRFLOW_HOME/airflow-venv"

# Use external DAG folder (relative to AIRFLOW_HOME)
export AIRFLOW__CORE__DAGS_FOLDER="$AIRFLOW_HOME/airflow-dags"

OUTPUT_DIR="$AIRFLOW_HOME/airflow-output"
mkdir -p "$OUTPUT_DIR"

DATE=$(date +%Y-%m-%d)

WEBSERVER_LOG="$OUTPUT_DIR/webserver-$DATE.log"
SCHEDULER_LOG="$OUTPUT_DIR/scheduler-$DATE.log"

# Activate venv
echo "📥 Activating venv and using Airflow $AIRFLOW_VERSION"
source "$VENV_DIR/bin/activate"

# Initialize DB if needed
airflow db upgrade >> "$OUTPUT_DIR/db-upgrade-$DATE.log" 2>&1

# Create default admin user if DEV_MODE is true
if [ "$DEV_MODE" = true ]; then
  if ! airflow users list | grep -q "admin"; then
    echo "DEV_MODE enabled → Creating default admin user (username=admin, password=$DEFAULT_ADMIN_PASSWORD)"
    airflow users create \
      --username admin \
      --firstname Airflow \
      --lastname Admin \
      --role Admin \
      --email "$ADMIN_EMAIL" \
      --password "$DEFAULT_ADMIN_PASSWORD"
  else
    echo "DEV_MODE enabled → Admin user already exists, skipping user creation."
  fi
else
  echo "DEV_MODE disabled → Skipping default admin user creation."
fi

# Start webserver
echo "🚀 Starting Airflow Webserver (logs: $WEBSERVER_LOG)..."
airflow webserver --port 8080 >> "$WEBSERVER_LOG" 2>&1 &

# Start scheduler
echo "🌀 Starting Airflow Scheduler (logs: $SCHEDULER_LOG)..."
airflow scheduler >> "$SCHEDULER_LOG" 2>&1 &

echo "✅ Airflow $AIRFLOW_VERSION is running"
echo "   AIRFLOW_HOME=$AIRFLOW_HOME"
echo "   DAGS_FOLDER=$AIRFLOW__CORE__DAGS_FOLDER"
echo "   Logs in $OUTPUT_DIR"
echo "   Login at http://localhost:8080 with username=admin and password=$DEFAULT_ADMIN_PASSWORD"