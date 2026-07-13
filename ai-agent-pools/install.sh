#!/usr/bin/env bash

set -Eeuo pipefail

###############################################################################
# AI Agent Pools
# Ubuntu 22.04 / Ubuntu 24.04
###############################################################################

PROJECT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_DIR="$PROJECT_HOME/.venv"
REQUIREMENTS_FILE="$PROJECT_HOME/requirements.txt"

export DAGSTER_HOME="$PROJECT_HOME/.dagster"

###############################################################################
# Colors
###############################################################################

GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[1;33m"
NC="\033[0m"

###############################################################################
# Detect Python
###############################################################################

find_python() {

    for py in python3.12 python3.11 python3.10 python3; do
        if command -v "$py" >/dev/null 2>&1; then
            echo "$py"
            return
        fi
    done

    echo ""
}

PYTHON=$(find_python)

if [ -z "$PYTHON" ]; then
    echo -e "${RED}Python 3 is not installed.${NC}"
    exit 1
fi

echo -e "${GREEN}Using $($PYTHON --version)${NC}"

###############################################################################
# Ensure venv support exists
###############################################################################

if ! "$PYTHON" -m venv --help >/dev/null 2>&1; then

    echo -e "${YELLOW}Installing python3-venv...${NC}"

    sudo apt-get update

    sudo apt-get install -y python3-venv

fi

###############################################################################
# Create virtual environment
###############################################################################

if [ ! -d "$VENV_DIR" ]; then

    echo -e "${GREEN}Creating virtual environment...${NC}"

    "$PYTHON" -m venv "$VENV_DIR"

else

    echo -e "${GREEN}Virtual environment already exists.${NC}"

fi

###############################################################################
# Activate
###############################################################################

source "$VENV_DIR/bin/activate"

###############################################################################
# Upgrade pip
###############################################################################

python -m pip install --upgrade \
    pip \
    setuptools \
    wheel

###############################################################################
# Install requirements
###############################################################################

if [ ! -f "$REQUIREMENTS_FILE" ]; then

    echo -e "${RED}requirements.txt not found.${NC}"

    exit 1

fi

echo -e "${GREEN}Installing dependencies...${NC}"

pip install -r "$REQUIREMENTS_FILE"

###############################################################################
# Dagster Home
###############################################################################

mkdir -p "$DAGSTER_HOME"

###############################################################################
# Verify installation
###############################################################################

echo
echo "Python : $(python --version)"
echo "Pip    : $(pip --version)"
echo "Dagster: $(dagster --version)"

###############################################################################
# Finish
###############################################################################

echo
echo "========================================================="
echo " AI Agent Pools is ready"
echo "========================================================="
echo
echo "Activate:"
echo
echo "    source .venv/bin/activate"
echo
echo "Run Dagster:"
echo
echo "    dagster dev"
echo
echo "Dagster UI:"
echo
echo "    http://localhost:3000"
echo
echo "DAGSTER_HOME:"
echo
echo "    $DAGSTER_HOME"
echo