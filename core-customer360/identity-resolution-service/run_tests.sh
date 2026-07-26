#!/bin/bash
# Runs the Customer Identity Resolution (CIR) Python test suite.
#
# Loads PostgreSQL connection settings from .env (see
# core-customer360/dev-start-pgsql.sh for the matching local Docker DB),
# creates/reuses a virtualenv, installs requirements.txt, then runs pytest.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ENV_FILE="${SCRIPT_DIR}/.env"
if [ -f "$ENV_FILE" ]; then
  echo "🔧 Loading database config from ${ENV_FILE}..."
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
else
  echo "⚠️  Warning: ${ENV_FILE} not found. Using default environment variables."
fi

VENV_DIR="${SCRIPT_DIR}/.venv"
if [ ! -d "$VENV_DIR" ]; then
  echo "📦 Creating virtual environment at ${VENV_DIR}..."
  python3 -m venv "$VENV_DIR"
fi

# shellcheck disable=SC1091
source "$VENV_DIR/bin/activate"

echo "📥 Installing requirements..."
pip install -q -r requirements.txt

echo "🧪 Running CIR tests (DB_HOST=${DB_HOST:-unset} DB_NAME=${DB_NAME:-unset} DB_SCHEMA=${DB_SCHEMA:-unset})..."
pytest -v "$@"
