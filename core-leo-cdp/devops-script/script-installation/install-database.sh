#!/bin/bash
set -e

# -----------------------------
# Install ArangoDB 3.11 Community on Ubuntu
# -----------------------------

# Add ArangoDB GPG key
curl -fsSL https://download.arangodb.com/arangodb311/DEBIAN/Release.key -o /tmp/arangodb-release.key
sudo mkdir -p /usr/share/keyrings
sudo gpg --dearmor /tmp/arangodb-release.key | sudo tee /usr/share/keyrings/arangodb-archive-keyring.gpg >/dev/null

# Add ArangoDB repository
echo "deb [signed-by=/usr/share/keyrings/arangodb-archive-keyring.gpg] https://download.arangodb.com/arangodb311/DEBIAN/ /" \
  | sudo tee /etc/apt/sources.list.d/arangodb.list

# Ensure HTTPS transport is available
sudo apt-get update
sudo apt-get install -y apt-transport-https

# Update package index
sudo apt-get update

# Install ArangoDB 3.11 Community
sudo apt-get install -y arangodb3=3.11.14-1

# Verify installation
arangod --version

echo "✅ ArangoDB 3.11 Community installed successfully."
