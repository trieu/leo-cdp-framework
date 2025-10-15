#!/usr/bin/env bash
set -e  # Stop on error

# Ensure system packages are up to date
sudo apt update -y
sudo apt install -y software-properties-common

# Add Certbot official PPA for the latest stable version
sudo add-apt-repository -y ppa:certbot/certbot

# Install Certbot with NGINX plugin
sudo apt update -y
sudo apt install -y certbot python3-certbot-nginx

# Enable and start NGINX if not running
sudo systemctl enable nginx || true
sudo systemctl start nginx || true

echo
echo "✅ Certbot and NGINX plugin installed successfully."

# === Request certificate section ===
# Example (non-interactive):
# sudo certbot --nginx -d example.com -d www.example.com --non-interactive --agree-tos -m admin@example.com

echo
echo "To issue a certificate, run something like:"
echo "  sudo certbot --nginx -d yourdomain.com -d www.yourdomain.com --non-interactive --agree-tos -m your@email.com"
echo
echo "To test renewal:"
echo "  sudo certbot renew --dry-run"
