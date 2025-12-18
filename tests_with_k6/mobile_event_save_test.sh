#!/usr/bin/env bash

set -e

# =============================
# REQUIRED ENV VARS (MOBILE SDK)
# =============================
missing=""

[ -z "$CDP_HOSTNAME" ] && missing="$missing CDP_HOSTNAME"
[ -z "$MAX_USER" ] && missing="$missing MAX_USER"
[ -z "$ACCESS_TOKEN_KEY" ] && missing="$missing ACCESS_TOKEN_KEY"
[ -z "$ACCESS_TOKEN_VALUE" ] && missing="$missing ACCESS_TOKEN_VALUE"

if [ -n "$missing" ]; then
    echo "ERROR: Missing required environment variables:$missing"
    echo
    echo "Usage example:"
    echo "  CDP_HOSTNAME=obs.example.com \\"
    echo "  ACCESS_TOKEN_KEY=7DMVDkI261aTPDgp6Oj5Ox \\"
    echo "  ACCESS_TOKEN_VALUE=6696371_5VSEAfjeGtI0ZF6kaC4P0m \\"
    echo "  MAX_USER=1000 \\"
    echo "  ./run_k6.sh"
    exit 1
fi

# =============================
# RUN INFO (MASK SECRETS)
# =============================
echo "Running k6 mobile SDK test with:"
echo "  CDP_HOSTNAME        = $CDP_HOSTNAME"
echo "  ACCESS_TOKEN_KEY    = ${ACCESS_TOKEN_KEY:0:6}******"
echo "  ACCESS_TOKEN_VALUE  = ${ACCESS_TOKEN_VALUE:0:6}******"
echo "  MAX_USER            = $MAX_USER"
echo

# =============================
# EXECUTE K6
# =============================
CDP_HOSTNAME="$CDP_HOSTNAME" \
ACCESS_TOKEN_KEY="$ACCESS_TOKEN_KEY" \
ACCESS_TOKEN_VALUE="$ACCESS_TOKEN_VALUE" \
MAX_USER="$MAX_USER" \
k6 run mobile_event_save_test.js
