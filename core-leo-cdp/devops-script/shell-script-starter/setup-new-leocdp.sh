#!/bin/bash
set -e

# -----------------------------
# LEO CDP System Setup Script
# -----------------------------

BUILD_VERSION="v_0.9.0"
JAR_MAIN="leo-main-starter-$BUILD_VERSION.jar"

# --- Check if JAR exists ---
if [ ! -f "$JAR_MAIN" ]; then
  echo "❌ Error: $JAR_MAIN not found in current directory."
  exit 1
fi

# --- Prompt for Super Admin Password ---
echo "🚀 LEO CDP System Setup"
echo "-------------------------"
read -rsp "Enter the superadmin password: " superadmin_password
echo ""

# Optional: confirm password
read -rsp "Confirm password: " superadmin_password_confirm
echo ""

if [ "$superadmin_password" != "$superadmin_password_confirm" ]; then
  echo "❌ Passwords do not match. Exiting."
  exit 1
fi

# --- Run setup ---
echo "🔑 Running system setup with username: superadmin"
java -jar "$JAR_MAIN" setup-system-with-password "$superadmin_password"

echo "✅ LEO CDP setup completed successfully."
