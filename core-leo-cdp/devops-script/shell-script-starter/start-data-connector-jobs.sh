#!/usr/bin/env bash
set -euo pipefail

# === CONFIGURATION ===
LEO_CDP_FOLDER="/build/cdp-instance"
BUILD_VERSION="v_0.9.0"
JOB_NAME="DataConnectorScheduler"
JAR_MAIN="leo-scheduler-starter-${BUILD_VERSION}.jar"

JVM_PARAMS="-Xms256m -Xmx1500m -XX:+TieredCompilation -XX:+UseCompressedOops -XX:+DisableExplicitGC -XX:+UseNUMA -server"

LOG_DIR="${LEO_CDP_FOLDER}/logs"
LOG_FILE="${LOG_DIR}/${JOB_NAME}.log"
MAX_LOG_DAYS=7  # keep compressed logs for 7 days

# === ENV PREP ===
mkdir -p "$LOG_DIR"
cd "$LEO_CDP_FOLDER" || { echo "Folder not found: $LEO_CDP_FOLDER"; exit 1; }

timestamp() {
  date '+%Y-%m-%d %H:%M:%S'
}

echo "[$(timestamp)] === Rotating and restarting $JOB_NAME ===" >> "$LOG_FILE"

# === LOG ROTATION ===
if [ -f "$LOG_FILE" ]; then
  ROTATED="${LOG_FILE}.$(date '+%Y%m%d-%H%M').gz"
  echo "[$(timestamp)] Rotating old log -> $ROTATED" >> "$LOG_FILE"
  gzip -c "$LOG_FILE" > "$ROTATED" && : > "$LOG_FILE"
fi

# Clean up old gzipped logs
find "$LOG_DIR" -type f -name "${JOB_NAME}.log.*.gz" -mtime +${MAX_LOG_DAYS} -delete

# === PROCESS CONTROL ===
# Kill any old process cleanly
PID=$(pgrep -f "$JOB_NAME" || true)
if [ -n "$PID" ]; then
  echo "[$(timestamp)] Found running $JOB_NAME process (PID: $PID), stopping..." >> "$LOG_FILE"
  kill -15 "$PID" || true
  sleep 3
fi

# Start new process
echo "[$(timestamp)] Starting $JOB_NAME..." >> "$LOG_FILE"
nohup java $JVM_PARAMS -jar "$JAR_MAIN" "$JOB_NAME" >> "$LOG_FILE" 2>&1 &

echo "[$(timestamp)] $JOB_NAME restarted (PID: $!)" >> "$LOG_FILE"
echo "[$(timestamp)] === Done ===" >> "$LOG_FILE"
