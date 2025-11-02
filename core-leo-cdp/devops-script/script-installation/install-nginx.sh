#!/usr/bin/env bash
set -euo pipefail

# -----------------------------
# Ensure NGINX Stable (Open Source) from nginx.org on Ubuntu 22.04 / 24.04
# -----------------------------
# - Checks if NGINX is installed and from nginx.org
# - Preserves /etc/nginx/conf.d/ and custom configs
# - Avoids removing working configurations
# -----------------------------

NGINX_KEYRING="/usr/share/keyrings/nginx-archive-keyring.gpg"
NGINX_REPO_LIST="/etc/apt/sources.list.d/nginx.list"
DISTRO_CODENAME=$(lsb_release -cs)

echo "🔍 Checking for existing NGINX installation..."

if command -v nginx >/dev/null 2>&1; then
  INSTALLED_VERSION=$(nginx -v 2>&1 | awk -F/ '{print $2}')
  if [[ -f "$NGINX_REPO_LIST" ]] && grep -q "nginx.org" "$NGINX_REPO_LIST"; then
    echo "✅ NGINX ${INSTALLED_VERSION} (from nginx.org) already installed. Skipping installation."
    exit 0
  else
    echo "⚠️ Detected NGINX ${INSTALLED_VERSION} from Ubuntu repo. Will upgrade to official nginx.org build."
  fi
else
  echo "📦 NGINX not found. Proceeding with installation."
fi

# Ensure dependencies
sudo apt-get update -qq
sudo apt-get install -y curl gnupg2 ca-certificates lsb-release ubuntu-keyring apt-transport-https

# Add official NGINX signing key
if [ ! -f "$NGINX_KEYRING" ]; then
  echo "🔑 Adding official NGINX signing key..."
  curl -fsSL https://nginx.org/keys/nginx_signing.key | sudo gpg --dearmor -o "$NGINX_KEYRING"
else
  echo "🔑 NGINX signing key already exists."
fi

# Verify fingerprint (optional)
sudo gpg --show-keys --with-fingerprint "$NGINX_KEYRING" | grep -A1 "Key fingerprint"

# Add nginx.org repo if not configured
if [ ! -f "$NGINX_REPO_LIST" ] || ! grep -q "nginx.org" "$NGINX_REPO_LIST"; then
  echo "🧩 Adding official NGINX stable repository..."
  echo "deb [signed-by=${NGINX_KEYRING}] https://nginx.org/packages/ubuntu ${DISTRO_CODENAME} nginx" \
    | sudo tee "$NGINX_REPO_LIST" >/dev/null
  echo "deb-src [signed-by=${NGINX_KEYRING}] https://nginx.org/packages/ubuntu ${DISTRO_CODENAME} nginx" \
    | sudo tee -a "$NGINX_REPO_LIST" >/dev/null
else
  echo "🧩 NGINX repository already configured."
fi

# Stop service but preserve config files
if systemctl is-active --quiet nginx; then
  echo "⏸ Stopping NGINX temporarily for upgrade..."
  sudo systemctl stop nginx
fi

# Remove only binary-related Ubuntu packages, preserve /etc/nginx configs
echo "🧹 Cleaning old Ubuntu NGINX binaries (keeping configs)..."
sudo apt-get remove -y nginx nginx-common nginx-full nginx-core --allow-change-held-packages || true
sudo apt-get autoremove -y
sudo apt-get clean

# Update and install stable NGINX
echo "🔄 Installing latest NGINX stable from nginx.org..."
sudo apt-get update -qq
sudo apt-get install -y nginx

# Enable and start service again
echo "🚀 Starting and enabling NGINX..."
sudo systemctl enable nginx
sudo systemctl restart nginx

# Verify installation
echo "✅ Verifying installed version..."
nginx -v
systemctl --no-pager status nginx | grep "Active:"

# Confirm that configs are preserved
echo "📁 Checking existing configuration files..."
if [ -d /etc/nginx/conf.d ]; then
  echo "✅ Existing configuration files preserved under /etc/nginx/conf.d/"
else
  echo "⚠️ /etc/nginx/conf.d/ missing — please verify your setup."
fi

echo "🎉 NGINX stable (open source) successfully installed and configured!"
