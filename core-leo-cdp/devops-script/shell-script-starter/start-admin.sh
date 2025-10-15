#!/usr/bin/env bash
set -euo pipefail

# === CONFIGURATION ===
LEO_CDP_FOLDER="/build/cdp-instance"
BUILD_VERSION="v_0.8.9"
JAR_MAIN="leo-main-starter-${BUILD_VERSION}.jar"

# Define all admin router keys
HTTP_ROUTER_KEYS=("admin1" "admin2" "admin3")

# Java VM tuning
JVM_PARAMS="-Xms256m -Xmx1G -XX:+TieredCompilation -XX:+UseCompressedOops -XX:+DisableExplicitGC -XX:+UseNUMA -server"

# === PREPARE ENVIRONMENT ===
if [ -n "${LEO_CDP_FOLDER:-}" ]; then
  echo "Switching to LEO_CDP_FOLDER: $LEO_CDP_FOLDER"
  cd "$LEO_CDP_FOLDER" || { echo "Folder not found: $LEO_CDP_FOLDER"; exit 1; }
else
  echo "Skipping directory change. Using current folder..."
fi

LOG_DIR="${LEO_CDP_FOLDER}/logs"
mkdir -p "$LOG_DIR"

# === START ADMIN OBSERVERS ===
for ADMIN_KEY in "${HTTP_ROUTER_KEYS[@]}"; do
  echo "--------------------------------------------------"
  echo "Starting admin worker: $ADMIN_KEY"

  # Kill existing process if running
  PID=$(pgrep -f "$ADMIN_KEY" || true)
  if [ -n "$PID" ]; then
    echo "Found existing process for $ADMIN_KEY (PID: $PID), stopping..."
    kill -15 "$PID" || true
    sleep 3
  fi

  # Log file handling
  LOG_FILE="${LOG_DIR}/admin-${ADMIN_KEY}.log"

  # Rotate log daily (compress old one if modified > 1 day ago)
  if [ -f "$LOG_FILE" ]; then
    LAST_MODIFIED=$(date -r "$LOG_FILE" +%s)
    NOW=$(date +%s)
    AGE_DAYS=$(( (NOW - LAST_MODIFIED) / 86400 ))

    if [ "$AGE_DAYS" -ge 1 ]; then
      ARCHIVE="${LOG_FILE}.$(date '+%Y%m%d').gz"
      echo "Rotating old log to $ARCHIVE"
      gzip -c "$LOG_FILE" > "$ARCHIVE"
      : > "$LOG_FILE"  # Truncate
    fi
  fi

  # Start new admin worker in background
  echo "Launching $ADMIN_KEY..."
  nohup java $JVM_PARAMS -jar "$JAR_MAIN" "$ADMIN_KEY" >> "$LOG_FILE" 2>&1 &

  echo "Admin worker '$ADMIN_KEY' started (PID: $!). Log: $LOG_FILE"
done

echo "--------------------------------------------------"
echo "✅ All admin workers started successfully at $(date)"
