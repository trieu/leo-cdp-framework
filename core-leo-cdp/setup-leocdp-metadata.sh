#!/bin/bash
set -e

# --- Metadata ---
SRC_FILE="setup-leocdp-metadata-tpl.properties"
DEST_FILE="leocdp-metadata.properties"

echo "──────────────────────────────────────────────"
echo "🚀 LEO CDP Production Config Setup"
echo "──────────────────────────────────────────────"

# --- Check sample file ---
if [ ! -f "$SRC_FILE" ]; then
  echo "❌ Missing template: $SRC_FILE"
  exit 1
fi

# --- Ask user for info ---
read -rp "Enter HTTP Admin Domain (e.g., admin.leocdp.yourdomain.com): " httpAdminDomain
read -rp "Enter WebSocket Domain (e.g., ws.leocdp.yourdomain.com): " webSocketDomain
read -rp "Enter Data Observer Domain (e.g., data.leocdp.yourdomain.com): " httpObserverDomain
read -rp "Enter LEO Bot Domain (e.g., bot.leocdp.yourdomain.com): " httpLeoBotDomain
read -rp "Enter LEO Bot API Key: " httpLeoBotApiKey
read -rp "Enter Super Admin Email: " superAdminEmail

read -rp "Enter Admin Logo URL (leave empty for default): " adminLogoUrl
if [ -z "$adminLogoUrl" ]; then
  adminLogoUrl="https://cdn.jsdelivr.net/gh/USPA-Technology/leo-cdp-static-files@latest/images/leo-cdp-logo.png"
fi

echo ""
echo "----- SMTP Configuration -----"
read -rp "SMTP Host: " smtpHost
read -rp "SMTP Port: " smtpPort
read -rp "SMTP User: " smtpUser
read -rp "SMTP Password: " smtpPassword
read -rp "SMTP From Address: " smtpFromAddress

echo ""
echo "----- Database Backup Configuration -----"
read -rp "Database Backup Path: " databaseBackupPath
read -rp "Backup Period Hours (default 24): " databaseBackupPeriodHours
databaseBackupPeriodHours=${databaseBackupPeriodHours:-24}
read -rp "Backup Retention Days (default 7): " databaseBackupRetentionDays
databaseBackupRetentionDays=${databaseBackupRetentionDays:-7}

echo ""
echo "Generating production config from $SRC_FILE ..."
sleep 1

# --- Replace placeholders in template ---
sed \
  -e "s|{{httpAdminDomain}}|${httpAdminDomain}|g" \
  -e "s|{{webSocketDomain}}|${webSocketDomain}|g" \
  -e "s|{{httpObserverDomain}}|${httpObserverDomain}|g" \
  -e "s|{{httpLeoBotDomain}}|${httpLeoBotDomain}|g" \
  -e "s|{{httpLeoBotApiKey}}|${httpLeoBotApiKey}|g" \
  -e "s|{{superAdminEmail}}|${superAdminEmail}|g" \
  -e "s|{{adminLogoUrl}}|${adminLogoUrl}|g" \
  -e "s|{{smtpHost}}|${smtpHost}|g" \
  -e "s|{{smtpPort}}|${smtpPort}|g" \
  -e "s|{{smtpUser}}|${smtpUser}|g" \
  -e "s|{{smtpPassword}}|${smtpPassword}|g" \
  -e "s|{{smtpFromAddress}}|${smtpFromAddress}|g" \
  -e "s|{{databaseBackupPath}}|${databaseBackupPath}|g" \
  -e "s|{{databaseBackupPeriodHours}}|${databaseBackupPeriodHours}|g" \
  -e "s|{{databaseBackupRetentionDays}}|${databaseBackupRetentionDays}|g" \
  "$SRC_FILE" > "$DEST_FILE"

echo "✅ Created $DEST_FILE"

# --- Add to .gitignore if not already ---
if ! grep -qxF "$DEST_FILE" .gitignore 2>/dev/null; then
  echo "$DEST_FILE" >> .gitignore
  echo "📁 Added $DEST_FILE to .gitignore"
fi

# --- Confirm result ---
echo "\n ───────────────────────────────────────────"
echo "✅ LEO CDP production metadata generated."
echo "📄 Path: $(realpath "$DEST_FILE")"
echo "──────────────────────────────────────────────"
echo "Preview of generated file:"
echo "──────────────────────────────────────────────"
head -n 30 "$DEST_FILE"
echo "──────────────────────────────────────────────"