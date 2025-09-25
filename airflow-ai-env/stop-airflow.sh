#!/bin/bash
set -euo pipefail

echo "🔎 Looking for running Airflow processes..."

PIDS=$(pgrep -f "airflow")

if [ -z "$PIDS" ]; then
  echo "✅ No Airflow processes found."
  exit 0
fi

echo "⚠️ Found Airflow processes: $PIDS"
echo "Sending SIGTERM (graceful stop)..."

kill -15 $PIDS

# Wait a few seconds and force kill if still alive
sleep 5
if pgrep -f "airflow" > /dev/null; then
  echo "⏳ Processes still alive, forcing kill..."
  pkill -9 -f "airflow"
fi

echo "✅ All Airflow processes stopped."
