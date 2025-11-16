#!/usr/bin/env bash

set -e

# Require env vars: CDP_HOSTNAME, EVENT_OBSERVER_ID, MAX_USER
missing=""

[ -z "$CDP_HOSTNAME" ] && missing="$missing CDP_HOSTNAME"
[ -z "$EVENT_OBSERVER_ID" ] && missing="$missing EVENT_OBSERVER_ID"
[ -z "$MAX_USER" ] && missing="$missing MAX_USER"

if [ -n "$missing" ]; then
    echo "ERROR: Missing required environment variables:$missing"
    echo "Usage example:"
    echo "  CDP_HOSTNAME=datahub.1invest.vn EVENT_OBSERVER_ID=xxxx MAX_USER=1000 ./run_k6.sh"
    exit 1
fi

echo "Running k6 with:"
echo "  CDP_HOSTNAME       = $CDP_HOSTNAME"
echo "  EVENT_OBSERVER_ID  = $EVENT_OBSERVER_ID"
echo "  MAX_USER           = $MAX_USER"

CDP_HOSTNAME="$CDP_HOSTNAME" \
EVENT_OBSERVER_ID="$EVENT_OBSERVER_ID" \
MAX_USER="$MAX_USER" \
k6 run user_session_load_test.js
