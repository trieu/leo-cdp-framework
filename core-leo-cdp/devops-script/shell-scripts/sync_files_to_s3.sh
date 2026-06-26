#!/bin/bash

# ==============================================================================
# Script Name: sync_files_to_s3.sh
# Description: Installs dependencies via sudo if missing, loads configurations
#              safely from a .env file, and strictly runs rsync as a normal user.
# ==============================================================================

# Get the directory where this script is physically located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# Catch if the user accidentally ran the ENTIRE script with sudo
if [ "$EUID" -eq 0 ] && [ -n "$SUDO_USER" ]; then
    echo "ERROR: Please do NOT run this script with 'sudo'."
    echo "Run it as a normal user: ./sync_files_to_s3.sh"
    echo "The script will ask for sudo permissions internally only if it needs to install packages."
    exit 1
fi

# Load Environment Variables safely from .env file
if [ -f "$ENV_FILE" ]; then
    echo "Loading configuration from $ENV_FILE..."
    # FIX: Safely read .env line-by-line, handling spaces, quotes, and comments correctly
    while IFS= read -r line || [ -n "$line" ]; do
        # Skip comments and empty lines
        [[ "$line" =~ ^[[:space:]]*# ]] && continue
        [[ -z "$line" ]] && continue
        # Export the variable
        eval "export $line"
    done < "$ENV_FILE"
else
    echo "ERROR: Configuration file '.env' not found at $ENV_FILE"
    echo "Please create a .env file with the following contents:"
    echo "----------------------------------------------------"
    echo "SRC_DIR=\"/build/cdp-data/data/publics/\""
    echo "DEST_DIR=\"/build/cdp-data/cloud-s3/bucket-name/publics/\""
    echo "----------------------------------------------------"
    exit 1
fi

# Validate that the variables were actually loaded and are not empty
if [ -z "$SRC_DIR" ] || [ -z "$DEST_DIR" ]; then
    echo "ERROR: SRC_DIR or DEST_DIR is empty in the .env file."
    exit 1
fi

# Function to check and install dependencies using sudo internally
check_and_install() {
    local pkg=$1
    if ! command -v "$pkg" &> /dev/null; then
        echo "Dependency '$pkg' is missing. Prompting for sudo to install it..."
        
        # Use sudo internally just for apt operations
        sudo apt-get update -y && sudo apt-get install -y "$pkg"
        
        if [ $? -eq 0 ]; then
            echo "Successfully installed $pkg."
        else
            echo "ERROR: Failed to install $pkg. Script aborted."
            exit 1
        fi
    else
        echo "Requirement check: '$pkg' is already installed."
    fi
}

echo "========================================="
echo "Checking System Dependencies..."
echo "========================================="

# Run dependency checks (will ask for sudo ONLY if missing)
check_and_install "rsync"
check_and_install "s3fs"

echo "========================================="
echo "Starting S3 Sync: $(date)"
echo "Running as user:  $(whoami)"
echo "Source:           $SRC_DIR"
echo "Destination:      $DEST_DIR"
echo "========================================="

# Verify source path exists
if [ ! -d "$SRC_DIR" ]; then
    echo "ERROR: Source directory $SRC_DIR does not exist!"
    exit 1
fi

# Verify destination path exists
if [ ! -d "$DEST_DIR" ]; then
    echo "ERROR: Destination directory $DEST_DIR is missing or S3 is not mounted!"
    exit 1
fi

echo "Running rsync..."
# Syncing files as normal user
rsync -rtv --ignore-existing "$SRC_DIR" "$DEST_DIR"

RSYNC_STATUS=$?

echo "========================================="
if [ $RSYNC_STATUS -eq 0 ]; then
    echo "SUCCESS: Sync completed flawlessly."
elif [ $RSYNC_STATUS -eq 23 ]; then
    echo "NOTICE: Sync completed, but some file attributes could not be set on S3. Files should be safely copied."
else
    echo "WARNING: rsync exited with errors (Exit Code: $RSYNC_STATUS)."
fi
echo "Finished at: $(date)"
echo "========================================="