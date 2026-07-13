#!/usr/bin/env bash

set -Eeuo pipefail

###############################################################################
# AI Agent Pools
# Stop Dagster Development Server
###############################################################################

GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
NC="\033[0m"

echo "Searching for Dagster processes..."

PIDS=$(pgrep -f "dagster dev" || true)

if [ -z "$PIDS" ]; then
    echo -e "${YELLOW}No Dagster development server is running.${NC}"
    exit 0
fi

echo "Stopping Dagster..."

for PID in $PIDS; do
    echo "  → PID $PID"
    kill "$PID"
done

sleep 2

REMAINING=$(pgrep -f "dagster dev" || true)

if [ -n "$REMAINING" ]; then
    echo -e "${YELLOW}Some processes are still running. Sending SIGKILL...${NC}"

    for PID in $REMAINING; do
        kill -9 "$PID"
    done
fi

echo -e "${GREEN}Dagster stopped successfully.${NC}"