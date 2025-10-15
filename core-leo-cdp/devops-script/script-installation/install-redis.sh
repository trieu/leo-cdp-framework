#!/usr/bin/env bash
set -e  # Exit immediately on error

# Update package index
sudo apt update -y

# Ensure HTTPS and certificates support
sudo apt install -y curl ca-certificates gnupg lsb-release

# Add the official Redis repository (for latest stable version)
curl -fsSL https://packages.redis.io/gpg | sudo gpg --dearmor -o /usr/share/keyrings/redis-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/redis-archive-keyring.gpg] https://packages.redis.io/deb $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/redis.list

# Update and install Redis
sudo apt update -y
sudo apt install -y redis

# Enable and start Redis service
sudo systemctl enable redis-server
sudo systemctl start redis-server

# Verify installation
echo "Redis version:"
redis-server --version
echo
echo "Redis service status:"
sudo systemctl --no-pager status redis-server
