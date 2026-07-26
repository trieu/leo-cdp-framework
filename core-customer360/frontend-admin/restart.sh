#!/usr/bin/env bash
###############################################################################
# Customer 360 Admin frontend (frontend-admin)
# Restarts the app: stop.sh followed by start.sh.
###############################################################################
set -Eeuo pipefail

PROJECT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_HOME"

ENV_FILE="$PROJECT_HOME/.env"
LOG_DIR="$PROJECT_HOME/logs"
LOG_FILE="$LOG_DIR/app.log"

GREEN="\033[0;32m"
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
# Load .env so FRONTEND_API_HOSTNAME (and other settings) can be reported
# before restart.
###############################################################################
if [ -f "$ENV_FILE" ]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
fi

log "API hostname for the browser: ${FRONTEND_API_HOSTNAME:-http://localhost:8000}"
log "${GREEN}Restarting frontend-admin...${NC}"
"$PROJECT_HOME/stop.sh"
"$PROJECT_HOME/start.sh"
