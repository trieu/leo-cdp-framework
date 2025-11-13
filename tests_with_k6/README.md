# 🧪 Setup & Load Testing Guide for LEO CDP

`k6` is a modern open-source load testing tool designed for API and backend performance testing.
This guide walks you through installing `k6` safely on **Ubuntu 22.04**, then running load tests against core endpoints of the **LEO CDP Event Observer**.

---

## 1. Install k6 (Automated Installer for Ubuntu 22.04)

Before installing, remove any old or corrupted k6 keyrings or repo definitions:

```bash
sudo rm -f /usr/share/keyrings/k6.gpg
sudo rm -f /usr/share/keyrings/k6-archive-keyring.gpg
sudo rm -f /etc/apt/sources.list.d/k6.list
```

Move into the testing folder and run the automated installer:

```bash
cd ./tests_with_k6
./install_k6_and_run_test.sh
```

This script handles:

* DNS validation
* Installation of the correct k6 GPG key
* Adding the official repository
* Installing the latest stable `k6`
* Creating sample test files
* Verifying installation

Once complete, `k6` is ready to use system-wide.

---

## 2. Run a Load Test: Initialize Visitor Session + Page View

To simulate the beginning of a real user journey, run your session-init test:

```bash
k6 run user_session_load_test.js
```

This script typically:

* Creates a **visitor session**
* Sends **page-view events**
* Checks API latency and error rates
* Produces a real-world baseline for CDP event ingestion

Use this test as the first building block for deeper scenarios like:

* Multi-page user flows
* Concurrent session ramping
* Stress and endurance tests
* RAG/AI assistant workload simulation

---

## 3. Check Current Limit and Apply System-Wide (systemd, k6, Docker, etc.)

To ensure higher file-descriptor limits apply to all systemd-managed services, update the global systemd configuration.

Open the systemd config file:

```bash
sudo nano /etc/systemd/system.conf
```

Find the line for `DefaultLimitNOFILE` and either uncomment it or add it if missing:

```
DefaultLimitNOFILE=65535
```

Reload systemd so the new limits take effect:

```bash
sudo systemctl daemon-reexec
```
