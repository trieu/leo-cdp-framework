#!/bin/bash
set -euo pipefail

# -----------------------------
# Install ArangoDB 3.11.14 Community on Ubuntu 22.04 / 24.04
# -----------------------------
# - Checks if ArangoDB is installed
# - Asks user confirmation for LEO CDP server
# - Installs ArangoDB 3.11.14 if not present
# -----------------------------

ARANGO_VERSION="3.11.14-1"
ARANGO_REPO_LIST="/etc/apt/sources.list.d/arangodb.list"
ARANGO_KEYRING_PATH="/usr/share/keyrings/arangodb-archive-keyring.gpg"

echo "🔍 Checking for existing ArangoDB installation..."

if command -v arangod >/dev/null 2>&1; then
  echo "✅ ArangoDB is already installed: $(arangod --version | grep -m1 'ArangoDB')"
  echo "Skipping installation."
  exit 0
fi

# Ask user if this server is for LEO CDP database
read -rp "❓ Is this server intended for the LEO CDP database? (y/n): " CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
  echo "🚫 Installation cancelled by user."
  exit 0
fi

echo "📦 Proceeding with ArangoDB ${ARANGO_VERSION} installation..."

# Ensure required dependencies
sudo apt-get update -qq
sudo apt-get install -y curl gnupg apt-transport-https ca-certificates

# Add ArangoDB GPG key if not already present
if [ ! -f "$ARANGO_KEYRING_PATH" ]; then
  echo "🔑 Adding ArangoDB GPG key..."
  curl -fsSL https://download.arangodb.com/arangodb311/DEBIAN/Release.key | sudo gpg --dearmor -o "$ARANGO_KEYRING_PATH"
else
  echo "🔑 GPG key already exists, skipping."
fi

# Add ArangoDB repository if not already configured
if [ ! -f "$ARANGO_REPO_LIST" ]; then
  echo "🧩 Adding ArangoDB repository..."
  echo "deb [signed-by=${ARANGO_KEYRING_PATH}] https://download.arangodb.com/arangodb311/DEBIAN/ /" \
    | sudo tee "$ARANGO_REPO_LIST" >/dev/null
else
  echo "🧩 Repository already configured."
fi

# Update package index
echo "🔄 Updating package index..."
sudo apt-get update -qq

# Install specific version of ArangoDB
echo "☕ Installing ArangoDB ${ARANGO_VERSION}..."
sudo apt-get install -y "arangodb3=${ARANGO_VERSION}"

# Verify installation
echo "✅ Verifying ArangoDB installation..."
arangod --version

echo "🎉 ArangoDB ${ARANGO_VERSION} installed successfully for LEO CDP."
