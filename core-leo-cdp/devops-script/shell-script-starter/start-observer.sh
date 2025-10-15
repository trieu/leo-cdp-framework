#!/usr/bin/env bash
set -euo pipefail

# === CONFIGURATION ===
LEO_CDP_FOLDER="/build/cdp-instance"
BUILD_VERSION="v_0.9.0"
JAR_MAIN="leo-observer-starter-${BUILD_VERSION}.jar"
JVM_PARAMS="-Xms256m -Xmx1500m -XX:+TieredCompilation -XX:+UseCompressedOops -XX:+DisableExplicitGC -XX:+UseNUMA -server"

# Define your router keys (space-separated)
HTTP_ROUTER_KEYS=("datahub")

# === PREPARE ENVIRONMENT ===
if [ -n "$LEO_CDP_FOLDER" ]; then
  echo "Switching to LEO_CDP_FOLDER: $LEO_CDP_FOLDER"
  cd "$LEO_CDP_FOLDER" || { echo "Folder not found: $LEO_CDP_FOLDER"; exit 1; }
fi

LOG_DIR="$LEO_CDP_FOLDER/logs"
mkdir -p "$LOG_DIR"

# === START OBSERVERS ===
for ROUTER_KEY in "${HTTP_ROUTER_KEYS[@]}"; do
  echo "--------------------------------------------------"
  echo "Starting observer for router key: $ROUTER_KEY"

  # Kill existing process if running
  PID=$(pgrep -f "$ROUTER_KEY" || true)
  if [ -n "$PID" ]; then
    echo "Killing existing process for $ROUTER_KEY (PID: $PID)"
    kill -15 "$PID" || true
    sleep 3
  fi

  LOG_FILE="$LOG_DIR/observer-${ROUTER_KEY}.log"
  echo "Log file: $LOG_FILE"

  # Rotate log if older than 1 day
  if [ -f "$LOG_FILE" ]; then
    MOD_DATE=$(date -r "$LOG_FILE" +%s)
    NOW=$(date +%s)
    AGE=$(( (NOW - MOD_DATE) / 86400 ))

    if [ "$AGE" -ge 1 ]; then
      ROTATED="$LOG_FILE.$(date '+%Y%m%d').gz"
      echo "Rotating old log -> $ROTATED"
      gzip -c "$LOG_FILE" > "$ROTATED"
      : > "$LOG_FILE"
    fi
  fi

  # Start the observer in background
  nohup java $JVM_PARAMS -jar "$JAR_MAIN" "$ROUTER_KEY" >> "$LOG_FILE" 2>&1 &

  echo "Observer for $ROUTER_KEY started (PID: $!)"
done

echo "--------------------------------------------------"
echo "✅ All observers started successfully at $(date)"
