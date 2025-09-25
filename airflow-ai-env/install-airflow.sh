#!/bin/bash

set -euo pipefail

# === CONFIG ===
AIRFLOW_HOME="$(pwd)"
PYTHON_VERSION="3.10"
VENV_DIR="$AIRFLOW_HOME/airflow-venv"
REQUIREMENTS_FILE="requirements.txt"
AIRFLOW_VERSION="2.11.0"

# === FUNCTIONS ===
check_python() {
  if ! command -v python$PYTHON_VERSION &>/dev/null; then
    echo "❌ Python $PYTHON_VERSION is not installed. Please install it first."
    exit 1
  fi
  echo "✅ Python $PYTHON_VERSION found: $(which python$PYTHON_VERSION)"
}

create_venv() {
  if [ ! -d "$VENV_DIR" ]; then
    echo "📦 Creating virtual environment at $VENV_DIR"
    python$PYTHON_VERSION -m venv "$VENV_DIR"
  else
    echo "⚡ Virtual environment already exists at $VENV_DIR"
  fi
}

install_airflow() {
  echo "📥 Activating venv and installing Airflow $AIRFLOW_VERSION"
  source "$VENV_DIR/bin/activate"

  pip install --upgrade pip setuptools wheel

  if [ -f "$REQUIREMENTS_FILE" ]; then
    echo "📥 Installing from $REQUIREMENTS_FILE"
    pip install -r "$REQUIREMENTS_FILE"
  else
    echo "⚠️ $REQUIREMENTS_FILE not found. Installing Airflow directly."
    pip install "apache-airflow==${AIRFLOW_VERSION}" \
      --constraint "https://raw.githubusercontent.com/apache/airflow/constraints-${AIRFLOW_VERSION}/constraints-${PYTHON_VERSION}.txt"
  fi

  echo "✅ Airflow installation complete."
}

# === MAIN ===
check_python
create_venv
install_airflow

echo
echo "🚀 Done! To activate Airflow environment, run:"
echo "   source $VENV_DIR/bin/activate"
echo
echo "👉 Initialize Airflow DB (first time only):"
echo "   airflow db init"
