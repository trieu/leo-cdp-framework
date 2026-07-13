#!/usr/bin/env bash

set -Eeuo pipefail

###############################################################################
# AI Agent Pools
# Start Dagster Development Server
###############################################################################

PROJECT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

VENV_DIR="$PROJECT_HOME/.venv"
ENV_FILE="$PROJECT_HOME/.env"

export DAGSTER_HOME="$PROJECT_HOME/.dagster"

HOST="0.0.0.0"
PORT="3000"

###############################################################################
# Colors
###############################################################################

GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
NC="\033[0m"

###############################################################################
# Activate Virtual Environment
###############################################################################

if [ ! -d "$VENV_DIR" ]; then
    echo -e "${RED}Virtual environment not found.${NC}"
    echo
    echo "Run:"
    echo "    ./setup.sh"
    exit 1
fi

source "$VENV_DIR/bin/activate"

###############################################################################
# Load .env
###############################################################################

if [ -f "$ENV_FILE" ]; then

    echo -e "${GREEN}Loading .env${NC}"

    set -a
    source "$ENV_FILE"
    set +a

fi

###############################################################################
# Create DAGSTER_HOME
###############################################################################

mkdir -p "$DAGSTER_HOME"

###############################################################################
# Verify Dagster
###############################################################################

if ! command -v dagster >/dev/null 2>&1; then
    echo -e "${RED}Dagster is not installed.${NC}"
    exit 1
fi

###############################################################################
# Show Environment
###############################################################################

echo
echo "========================================================="
echo " AI Agent Pools"
echo "========================================================="
echo
echo "Python        : $(python --version)"
echo "Dagster       : $(dagster --version)"
echo "Project       : $PROJECT_HOME"
echo "DAGSTER_HOME  : $DAGSTER_HOME"
echo "Host          : $HOST"
echo "Port          : $PORT"

if [ -n "${REDIS_HOST:-}" ]; then
    echo "Redis         : ${REDIS_HOST}:${REDIS_PORT}"
fi

echo
echo "Opening Dagster..."
echo

###############################################################################
# Start Dagster
###############################################################################

exec dagster dev \
    --host "$HOST" \
    --port "$PORT"