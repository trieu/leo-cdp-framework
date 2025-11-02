#!/bin/bash
set -euo pipefail

# -----------------------------
# Install Amazon Corretto 11 (Java 11) on Ubuntu 22.04 / 24.04
# -----------------------------
# This script:
#  - Imports the official Amazon Corretto GPG key
#  - Adds the Corretto APT repository
#  - Installs the JDK and verifies installation
# -----------------------------

echo "📦 Starting installation of Amazon Corretto 11 JDK..."

# Ensure dependencies for apt and curl are present
sudo apt-get update -qq
sudo apt-get install -y curl gnupg apt-transport-https ca-certificates software-properties-common

# Download and import Corretto GPG key
CORRETTO_KEY_URL="https://apt.corretto.aws/corretto.key"
CORRETTO_KEYRING_PATH="/usr/share/keyrings/corretto-archive-keyring.gpg"

if [ ! -f "$CORRETTO_KEYRING_PATH" ]; then
  echo "🔑 Adding Corretto GPG key..."
  curl -fsSL "$CORRETTO_KEY_URL" | sudo gpg --dearmor -o "$CORRETTO_KEYRING_PATH"
else
  echo "🔑 GPG key already present, skipping."
fi

# Add Corretto repository if not already present
if ! grep -q "^deb .*apt.corretto.aws" /etc/apt/sources.list.d/corretto.list 2>/dev/null; then
  echo "🧩 Adding Corretto repository..."
  echo "deb [signed-by=${CORRETTO_KEYRING_PATH}] https://apt.corretto.aws stable main" \
    | sudo tee /etc/apt/sources.list.d/corretto.list >/dev/null
else
  echo "🧩 Corretto repository already configured."
fi

# Update package index
echo "🔄 Updating package index..."
sudo apt-get update -qq

# Install Corretto JDK
echo "☕ Installing Amazon Corretto 11..."
sudo apt-get install -y java-11-amazon-corretto-jdk fontconfig

# Verify installation
echo "✅ Verifying Java installation..."
java -version
javac -version

echo "🎉 Amazon Corretto 11 (Java 11) installed successfully!"
