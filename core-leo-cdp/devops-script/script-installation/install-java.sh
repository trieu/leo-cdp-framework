#!/bin/bash
set -e

# -----------------------------
# Install Amazon Corretto 11 (Java 11) on Ubuntu
# -----------------------------

# Add Corretto GPG key
curl -fsSL https://apt.corretto.aws/corretto.key -o /tmp/corretto.key
sudo mkdir -p /usr/share/keyrings
sudo gpg --dearmor /tmp/corretto.key | sudo tee /usr/share/keyrings/corretto-archive-keyring.gpg >/dev/null

# Add Corretto repository
echo "deb [signed-by=/usr/share/keyrings/corretto-archive-keyring.gpg] https://apt.corretto.aws stable main" \
  | sudo tee /etc/apt/sources.list.d/corretto.list

# Update package index
sudo apt-get update

# Install Java 11 Amazon Corretto JDK
sudo apt-get install -y java-11-amazon-corretto-jdk

# Install fontconfig for proper font rendering
sudo apt-get install -y fontconfig

# Verify installation
java -version
javac -version

echo "✅ Amazon Corretto 11 (Java 11) installed successfully."