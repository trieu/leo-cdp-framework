#!/usr/bin/env bash
set -euo pipefail

# === CONFIGURATION ===
ENDPOINT_URL="https://datahub4uspa.leocdp.net/ping"
APP_USER="leocdp"
APP_DIR="/build/cdp-instance"
START_SCRIPT="start-observer.sh"
LOG_DIR="/var/log/leo-cdp"
LOG_FILE="${LOG_DIR}/observer-monitor.log"

# === PREPARE LOGGING ===
mkdir -p "$LOG_DIR"
exec >>"$LOG_FILE" 2>&1

timestamp() {
  date '+%Y-%m-%d %H:%M:%S'
}

echo "[$(timestamp)] Checking endpoint: $ENDPOINT_URL"

# === HEALTH CHECK ===
response_code=$(curl -s --max-time 10 -o /dev/null -w "%{http_code}" "$ENDPOINT_URL" || echo "000")

if [ "$response_code" -eq 200 ]; then
  echo "[$(timestamp)] ✅ Service is healthy (HTTP 200)."
else
  echo "[$(timestamp)] ⚠️  Service unhealthy (HTTP $response_code). Restarting observer..."

  # Use system-safe sudo with no password prompts; ensure crontab user has rights.
  if sudo -u "$APP_USER" bash -c "cd '$APP_DIR' && ./'$START_SCRIPT'"; then
    echo "[$(timestamp)] 🔁 Restart command executed successfully."
  else
    echo "[$(timestamp)] ❌ Failed to restart observer."
  fi
fi

# Rotate logs older than 7 days
find "$LOG_DIR" -type f -name "*.log" -mtime +7 -delete
