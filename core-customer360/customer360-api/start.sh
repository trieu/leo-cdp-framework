#!/usr/bin/env bash
###############################################################################
# Customer 360 / Identity Resolution API
# Starts the FastAPI app (app.py) with uvicorn in the background.
###############################################################################
set -Eeuo pipefail

PROJECT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_HOME"

VENV_DIR="$PROJECT_HOME/.venv"
ENV_FILE="$PROJECT_HOME/.env"
LOG_DIR="$PROJECT_HOME/logs"
PID_FILE="$PROJECT_HOME/.uvicorn.pid"
LOG_FILE="$LOG_DIR/app.log"

GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
NC="\033[0m"

###############################################################################
# Refuse to start twice
###############################################################################
if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo -e "${YELLOW}Already running (PID $(cat "$PID_FILE")). Use ./stop.sh first.${NC}"
    exit 0
fi

###############################################################################
# Virtual environment (create on first run, then reuse)
###############################################################################
if [ ! -d "$VENV_DIR" ]; then
    echo -e "${GREEN}Creating virtual environment at ${VENV_DIR}...${NC}"
    python3 -m venv "$VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

echo "Installing requirements..."
pip install -q -r requirements.txt

###############################################################################
# Load .env
###############################################################################
if [ -f "$ENV_FILE" ]; then
    echo -e "${GREEN}Loading ${ENV_FILE}...${NC}"
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
else
    echo -e "${YELLOW}Warning: ${ENV_FILE} not found. Using default environment variables.${NC}"
fi

HOST="${API_HOST:-0.0.0.0}"
PORT="${API_PORT:-8000}"
RELOAD_FLAG=""
if [ "${UVICORN_RELOAD:-false}" = "true" ]; then
    RELOAD_FLAG="--reload"
fi

mkdir -p "$LOG_DIR"

###############################################################################
# Start uvicorn in the background
###############################################################################
echo "Starting Customer 360 API on http://${HOST}:${PORT} ..."
nohup uvicorn app:app --host "$HOST" --port "$PORT" $RELOAD_FLAG >>"$LOG_FILE" 2>&1 &
echo $! >"$PID_FILE"

sleep 2

if kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo -e "${GREEN}Started (PID $(cat "$PID_FILE")). Logs: ${LOG_FILE}${NC}"
    echo "Health check:  curl http://${HOST}:${PORT}/health"
    echo "Docs:          http://${HOST}:${PORT}/docs"
else
    echo -e "${RED}Failed to start -- check ${LOG_FILE}${NC}"
    rm -f "$PID_FILE"
    exit 1
fi
