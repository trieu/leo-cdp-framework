#!/bin/bash

set -e

echo "====================================================="
echo "🚀 Starting Automated k6 Installation for Ubuntu 22.04"
echo "====================================================="

# -----------------------------------------
# 0. Check DNS first (Cloudflare fallback)
# -----------------------------------------
echo "🔍 Checking DNS resolution..."
if ! ping -c1 google.com >/dev/null 2>&1; then
    echo "⚠️ DNS resolution failed — applying fallback (1.1.1.1)..."
    echo "nameserver 1.1.1.1" | sudo tee /etc/resolv.conf > /dev/null
    sleep 1

    if ! ping -c1 google.com >/dev/null 2>&1; then
        echo "❌ DNS still failing after fallback. Aborting."
        exit 1
    fi

    echo "✅ DNS fixed using Cloudflare."
else
    echo "✅ DNS OK."
fi

# -----------------------------------------
# 1. Update system
# -----------------------------------------
echo "📦 Updating system packages..."
sudo apt-get update
sudo apt-get upgrade -y

# -----------------------------------------
# 2. Download NEW official k6 key and convert to keyring
# -----------------------------------------
echo "🔑 Downloading NEW official k6 GPG key..."

curl -fsSL https://dl.k6.io/key.gpg \
  | sudo gpg --dearmor -o /usr/share/keyrings/k6.gpg

echo "✅ k6 signing key installed: /usr/share/keyrings/k6.gpg"

# -----------------------------------------
# 3. Fix old repo file if exists
# -----------------------------------------
sudo rm -f /etc/apt/sources.list.d/k6.list

# -----------------------------------------
# 4. Add k6 APT repository (correct signed-by)
# -----------------------------------------
echo "📝 Adding k6 APT repository..."

echo "deb [signed-by=/usr/share/keyrings/k6.gpg] https://dl.k6.io/deb stable main" \
  | sudo tee /etc/apt/sources.list.d/k6.list >/dev/null

echo "Repository added:"
cat /etc/apt/sources.list.d/k6.list

# -----------------------------------------
# 5. Install k6
# -----------------------------------------
echo "📦 Installing k6..."
sudo apt-get update
sudo apt-get install k6 -y

echo "✅ k6 installed."

# -----------------------------------------
# 6. Verify installation
# -----------------------------------------
echo "🔍 Verifying installation..."
k6 version || { echo "❌ k6 failed to run."; exit 1; }

echo "✅ Installed k6 version: $(k6 version)"

# -----------------------------------------
# 7. Create datahub4dcdp.js
# -----------------------------------------
echo "🧪 Creating 'datahub4dcdp.js'..."

cat << 'EOF' > datahub4dcdp.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '10s', target: 10 }
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01']
  },
};

export default function () {
  const url = 'https://datahub4dcdp.bigdatavietnam.org/ping';

  const res = http.get(url);

  check(res, {
    'status 200': (r) => r.status === 200,
    'response < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.3);
}
EOF

echo "✅ Load test file created: ./datahub4dcdp.js"

# -----------------------------------------
# 8. Finish
# -----------------------------------------
echo "====================================================="
echo "🎉 Setup complete! Sleep in 5 seconds and we can do 'k6 run datahub4dcdp.js' "
sleep 5
k6 run datahub4dcdp.js
echo "====================================================="