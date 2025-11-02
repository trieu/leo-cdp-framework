#!/usr/bin/env bash
set -e  # Exit on error

echo "🔍 Checking Certbot installation on Ubuntu 22..."

# Check if certbot already exists
if command -v certbot >/dev/null 2>&1; then
  echo "✅ Certbot already installed: $(certbot --version)"
else
  echo "📦 Installing Certbot via Snap (recommended for Ubuntu 22+)..."
  
  # Ensure snapd exists and is running
  sudo apt update -y
  sudo apt install -y snapd

  # Make sure snap’s core is up to date
  sudo snap install core
  sudo snap refresh core

  # Remove any old APT Certbot version (if it exists)
  sudo apt remove -y certbot || true

  # Install the official Certbot snap
  sudo snap install --classic certbot

  # Create a symlink so `certbot` works as a system command
  sudo ln -sf /snap/bin/certbot /usr/bin/certbot
fi

# === Ensure NGINX exists and is running ===
if ! command -v nginx >/dev/null 2>&1; then
  echo "📦 Installing NGINX (stable open source version)..."
  sudo apt update -y
  sudo apt install -y nginx
else
  echo "✅ NGINX already installed."
fi

sudo systemctl enable nginx || true
sudo systemctl start nginx || true

echo
echo "✅ Certbot and NGINX are ready on Ubuntu 22.04+"
echo

# === Instructions ===
cat <<'EOF'
To issue a new certificate, run:
  sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com \
    --non-interactive --agree-tos -m your@email.com

To test automatic renewal:
  sudo certbot renew --dry-run
EOF
