#!/usr/bin/env bash
###############################################################################
# Customer 360 / Identity Resolution API
# Stops the uvicorn process started by start.sh.
###############################################################################
set -Eeuo pipefail

PROJECT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_FILE="$PROJECT_HOME/.uvicorn.pid"

GREEN="\033[0;32m"
YELLOW="\033[1;33m"
NC="\033[0m"

PID=""
if [ -f "$PID_FILE" ]; then
    PID="$(cat "$PID_FILE")"
fi

# Fallback: find it by command line if the PID file is missing/stale.
if [ -z "$PID" ] || ! kill -0 "$PID" 2>/dev/null; then
    PID="$(pgrep -f "uvicorn app:app" || true)"
fi

if [ -z "$PID" ]; then
    echo -e "${YELLOW}No running Customer 360 API process found.${NC}"
    rm -f "$PID_FILE"
    exit 0
fi

echo "Stopping Customer 360 API (PID $PID)..."
kill "$PID" 2>/dev/null || true

for _ in 1 2 3 4 5; do
    if ! kill -0 "$PID" 2>/dev/null; then
        break
    fi
    sleep 1
done

if kill -0 "$PID" 2>/dev/null; then
    echo -e "${YELLOW}Still running -- sending SIGKILL...${NC}"
    kill -9 "$PID" 2>/dev/null || true
fi

rm -f "$PID_FILE"
echo -e "${GREEN}Stopped.${NC}"
