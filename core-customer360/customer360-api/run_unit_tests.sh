#!/usr/bin/env bash
###############################################################################
# Customer 360 / Identity Resolution API -- unit test runner.
#
# Runs the hermetic unit test suite in tests/ (auth middleware, Keycloak
# login provisioning, multi-tenant Row-Level Security GUC wiring, generic
# tenant-scoped CRUD router). No real PostgreSQL/Redis/Keycloak is required
# -- every external dependency is faked/mocked (see tests/conftest.py).
#
# Usage:
#   ./run_unit_tests.sh                # run the whole suite
#   ./run_unit_tests.sh -k keycloak     # pass extra args straight to pytest
#   ./run_unit_tests.sh tests/test_auth_middleware.py
###############################################################################
set -Eeuo pipefail

PROJECT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_HOME"

GREEN="\033[0;32m"
YELLOW="\033[1;33m"
NC="\033[0m"

VENV_DIR="$PROJECT_HOME/.venv"

###############################################################################
# Virtual environment (create on first run, then reuse)
###############################################################################
if [ ! -d "$VENV_DIR" ]; then
    echo -e "${GREEN}Creating virtual environment at ${VENV_DIR}...${NC}"
    python3 -m venv "$VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

echo -e "${GREEN}Installing requirements...${NC}"
pip install -q -r requirements.txt

###############################################################################
# These are pure unit tests: no DB/Redis/Keycloak connection is required
# (everything is faked in tests/conftest.py). SSO_LOGIN is forced true so
# auth-required behavior is exercised the same way regardless of the local
# .env (which typically has SSO_LOGIN=false for interactive dev use).
###############################################################################
export SSO_LOGIN=true

echo -e "${YELLOW}Running customer360-api unit tests...${NC}"
python -m pytest tests/ -v "$@"
