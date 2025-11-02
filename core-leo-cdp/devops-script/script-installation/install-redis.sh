#!/usr/bin/env bash
set -euo pipefail

# ------------------------------------------------------------------
# Install Redis 8.x (Open Source) on Ubuntu 22.04 / 24.04
# Removes any old Redis versions, unholds packages, and installs
# the latest 8.x version available from packages.redis.io.
# ------------------------------------------------------------------

# Set the major version you are targeting
TARGET_MAJOR="8"
REDIS_REPO_LIST="/etc/apt/sources.list.d/redis.list"
REDIS_KEYRING="/usr/share/keyrings/redis-archive-keyring.gpg"
CODENAME=$(lsb_release -cs)

echo "🧪 Installing Redis ${TARGET_MAJOR}.x (removing older versions first)..."

# 0. Basic tools
sudo apt-get update -qq
sudo apt-get install -y curl ca-certificates gnupg lsb-release apt-transport-https

# 1. Stop and purge existing redis-server
if systemctl list-units --type=service | grep -q redis-server; then
  echo "🛑 Stopping existing Redis service..."
  sudo systemctl stop redis-server || true
fi

echo "🧹 Cleaning old Redis packages..."
sudo apt-mark unhold redis-server redis-tools 2>/dev/null || true
sudo apt-get remove --purge -y redis-server redis-tools --allow-change-held-packages || true
sudo apt-get autoremove -y
sudo apt-get clean

# Remove old config/data if needed
sudo rm -rf /etc/redis /var/lib/redis 2>/dev/null || true

# 2. Add official Redis repo and key
if [ ! -f "$REDIS_KEYRING" ]; then
  echo "🔑 Adding Redis GPG key..."
  curl -fsSL https://packages.redis.io/gpg | sudo gpg --dearmor -o "$REDIS_KEYRING"
fi

if [ ! -f "$REDIS_REPO_LIST" ]; then
  echo "🧩 Adding Redis APT repository..."
  echo "deb [signed-by=${REDIS_KEYRING}] https://packages.redis.io/deb ${CODENAME} main" \
    | sudo tee "$REDIS_REPO_LIST" >/dev/null
fi

sudo apt-get update -qq

# 3. Find available Redis package matching the target major version
# Note: This finds the latest package starting with "8."
AVAILABLE_PKG=$(apt-cache madison redis-server | grep "${TARGET_MAJOR}\." | head -n1 | awk '{print $3}')

if [ -z "$AVAILABLE_PKG" ]; then
  echo "⚠️ No explicit Redis ${TARGET_MAJOR}.x version found. Installing latest available from packages.redis.io."
  sudo apt-get install -y redis-server redis-tools --allow-change-held-packages
else
  echo "📦 Installing Redis ${AVAILABLE_PKG}..."
  sudo apt-get install -y --allow-downgrades --allow-change-held-packages \
    "redis-server=${AVAILABLE_PKG}" "redis-tools=${AVAILABLE_PKG}"
fi

# 4. Enable & start Redis
sudo systemctl enable redis-server
sudo systemctl restart redis-server

# 5. Verify version
# Use redis-cli as redis-server --version might not be in PATH for non-root
INSTALLED=$(redis-cli --version | awk '{print $2}')
echo "ℹ️ Installed Redis version: ${INSTALLED}"

if [[ "$INSTALLED" == ${TARGET_MAJOR}* ]]; then
  echo "🎉 Redis ${INSTALLED} installed successfully!"
else
  echo "❌ Unexpected version installed: ${INSTALLED} (was targeting ${TARGET_MAJOR}.x)"
  # Depending on your needs, you might want to exit here
  # exit 1
fi

# 6. Pin the installed version
echo "🔒 Pinning Redis packages to prevent unintended upgrades..."
sudo apt-mark hold redis-server redis-tools

# 7. Final check
echo
sudo systemctl --no-pager status redis-server | grep Active:
redis-cli ping
echo "✅ Installation complete and verified."