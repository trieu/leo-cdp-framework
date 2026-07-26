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

mkdir -p "$LOG_DIR"

# Echo to the console and append a timestamped, color-free copy to LOG_FILE.
log() {
    local msg="$1"
    echo -e "$msg"
    echo "$(date '+%Y-%m-%d %H:%M:%S') $msg" | sed -E 's/\x1b\[[0-9;]*m//g' >>"$LOG_FILE"
}

###############################################################################
# Refuse to start twice
###############################################################################
if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    log "${YELLOW}Already running (PID $(cat "$PID_FILE")). Use ./stop.sh first.${NC}"
    exit 0
fi

###############################################################################
# Virtual environment (create on first run, then reuse)
###############################################################################
if [ ! -d "$VENV_DIR" ]; then
    log "${GREEN}Creating virtual environment at ${VENV_DIR}...${NC}"
    python3 -m venv "$VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

log "Installing requirements..."
pip install -q -r requirements.txt

###############################################################################
# Ensure .env exists (symlink to ../.env if missing) -- dev mode only, skip
# when running inside a Docker container.
###############################################################################
if [ ! -f "$ENV_FILE" ] && [ ! -L "$ENV_FILE" ] && [ ! -f /.dockerenv ]; then
    if [ -f "$PROJECT_HOME/../.env" ]; then
        log "${YELLOW}${ENV_FILE} not found. Creating symlink to ../.env...${NC}"
        ln -s ../.env "$ENV_FILE"
    fi
fi

###############################################################################
# Load .env
###############################################################################
if [ -f "$ENV_FILE" ]; then
    log "${GREEN}Loading ${ENV_FILE}...${NC}"
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
else
    log "${YELLOW}Warning: ${ENV_FILE} not found. Using default environment variables.${NC}"
fi

###############################################################################
# SSO_LOGIN controls whether the API enforces Keycloak auth on requests.
# This is security-critical, so it must always be visible in the log.
###############################################################################
if [ "${SSO_LOGIN:-false}" = "true" ]; then
    log "${GREEN}SSO_LOGIN=true -- API authentication is ENABLED (Keycloak token required).${NC}"
else
    log "${RED}SSO_LOGIN=false -- API authentication is DISABLED (all requests allowed).${NC}"
fi

HOST="${API_HOST:-0.0.0.0}"
PORT="${API_PORT:-8000}"
RELOAD_FLAG=""
if [ "${UVICORN_RELOAD:-false}" = "true" ]; then
    RELOAD_FLAG="--reload"
fi

###############################################################################
# Start uvicorn in the background
###############################################################################
log "Starting Customer 360 API on http://${HOST}:${PORT} ..."
nohup uvicorn app:app --host "$HOST" --port "$PORT" $RELOAD_FLAG >>"$LOG_FILE" 2>&1 &
echo $! >"$PID_FILE"

sleep 2

if kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    log "${GREEN}Started (PID $(cat "$PID_FILE")). Logs: ${LOG_FILE}${NC}"
    echo "Health check:  curl http://${HOST}:${PORT}/health"
    echo "Docs:          http://${HOST}:${PORT}/docs"
else
    log "${RED}Failed to start -- check ${LOG_FILE}${NC}"
    rm -f "$PID_FILE"
    exit 1
fi
