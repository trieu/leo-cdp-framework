#!/bin/bash
set -e

# ==============================
# 🔧 CONFIGURATION
# ==============================

# Load environment variables from .env file
ENV_FILE="$(dirname "$0")/.env"
if [ -f "$ENV_FILE" ]; then
  echo -e "\e[36m📄 Loading environment from $ENV_FILE\e[0m"
  export $(grep -v '^#' "$ENV_FILE" | xargs)
else
  echo -e "\e[33m⚠️  No .env file found — using default values\e[0m"
fi

# ==============================
# 🚀 STARTUP SEQUENCE
# ==============================

echo -e "\n\e[34m🔄 Stopping old Keycloak container...\e[0m"
docker stop keycloak >/dev/null 2>&1 || true

echo -e "\e[34m🧹 Removing old container...\e[0m"
docker rm keycloak >/dev/null 2>&1 || true

echo -e "\e[32m🚀 Starting Keycloak $KEYCLOAK_VERSION on port $KEYCLOAK_PORT ...\e[0m"

docker run -d \
  --name keycloak \
  -p ${KEYCLOAK_PORT:-8080}:8080 \
  -e KC_BOOTSTRAP_ADMIN_USERNAME=${KC_BOOTSTRAP_ADMIN_USERNAME:-admin} \
  -e KC_BOOTSTRAP_ADMIN_PASSWORD=${KC_BOOTSTRAP_ADMIN_PASSWORD:-admin} \
  -e KC_PROXY=${KC_PROXY:-edge} \
  -e KC_HOSTNAME_STRICT=${KC_HOSTNAME_STRICT:-false} \
  -e KC_HOSTNAME_STRICT_HTTPS=${KC_HOSTNAME_STRICT_HTTPS:-true} \
  -e KC_HTTP_ENABLED=${KC_HTTP_ENABLED:-true} \
  -e KC_HTTP_RELATIVE_PATH=${KC_HTTP_RELATIVE_PATH:-/} \
  -e KC_HOSTNAME_URL=${KC_HOSTNAME_URL:-https://leoid.example.com} \
  -e KC_PROXY_HEADERS=${KC_PROXY_HEADERS:-xforwarded} \
  quay.io/keycloak/keycloak:${KEYCLOAK_VERSION:-26.4.2} \
  start

# ==============================
# 📊 LOGGING
# ==============================

echo -e "\n\e[36m📜 Docker container started successfully!\e[0m"
echo -e "\e[32m🌐 Access Keycloak at: ${KC_HOSTNAME_URL:-https://leoid.example.com}\e[0m"
echo -e "\e[36m🪵 Showing live logs (Ctrl+C to exit)\e[0m\n"

sleep 3
docker logs -f keycloak
