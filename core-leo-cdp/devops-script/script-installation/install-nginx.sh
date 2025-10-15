#!/usr/bin/env bash
set -e  # Stop on error

# Ensure the system is up to date
sudo apt update -y
sudo apt install -y curl gnupg2 ca-certificates lsb-release ubuntu-keyring

# Import NGINX signing key securely (apt-key is deprecated)
curl -fsSL https://nginx.org/keys/nginx_signing.key | sudo gpg --dearmor -o /usr/share/keyrings/nginx-archive-keyring.gpg

# Verify the key fingerprint (optional but recommended)
# The expected fingerprint (as of official NGINX docs) is:
# ABF5BD827BD9BF62
sudo gpg --show-keys --with-fingerprint /usr/share/keyrings/nginx-archive-keyring.gpg

# Add NGINX stable repository for your Ubuntu version
DISTRO_CODENAME=$(lsb_release -cs)
echo "deb [signed-by=/usr/share/keyrings/nginx-archive-keyring.gpg] https://nginx.org/packages/ubuntu/ ${DISTRO_CODENAME} nginx" | sudo tee /etc/apt/sources.list.d/nginx.list
echo "deb-src [signed-by=/usr/share/keyrings/nginx-archive-keyring.gpg] https://nginx.org/packages/ubuntu/ ${DISTRO_CODENAME} nginx" | sudo tee -a /etc/apt/sources.list.d/nginx.list

# Remove any old nginx package (Ubuntu’s own)
sudo apt remove -y nginx-common nginx-full nginx-core || true

# Install the stable version from NGINX.org
sudo apt update -y
sudo apt install -y nginx

# Enable and start the service
sudo systemctl enable nginx
sudo systemctl start nginx

# Print version to confirm installation
nginx -v
