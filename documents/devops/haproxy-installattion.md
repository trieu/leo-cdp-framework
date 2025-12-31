
# HAProxy 2.8 Installation & Deployment Guide

**LEO CDP – Internet Load Balancer Layer**

---

## 1. Purpose & Scope

This document describes how to install **HAProxy 2.8 (LTS)** using the official maintained PPA and deploy it as the **Internet-facing load balancer** for LEO CDP Admin.

**HAProxy responsibilities in this architecture:**

* Listen on **ports 80 and 443**
* Terminate **TLS / SSL**
* Enforce **HTTP → HTTPS redirect**
* Forward traffic to **NGINX (internal reverse proxy)**
* Do **NOT** perform application or Java worker load balancing

NGINX remains responsible for:

* Static content
* Reverse proxy logic
* Java service load balancing

---

## 2. Target Environment

* OS: **Ubuntu 20.04 / 22.04 LTS**
* Architecture: amd64
* Privileges: `sudo`
* TLS certificates: Let’s Encrypt (already issued)

---

## 3. Install HAProxy 2.8 (Official PPA)

### 3.1 Add HAProxy Maintainer Repository

HAProxy versions in Ubuntu default repos are usually outdated.
We use **Vincent Bernat’s official PPA**, maintained by the HAProxy core team.

```bash
sudo add-apt-repository ppa:vbernat/haproxy-2.8
sudo apt update
```

---

### 3.2 Install HAProxy

```bash
sudo apt install -y haproxy
```

---

### 3.3 Verify Installation

```bash
haproxy -v
```

Expected output (example):

```
HAProxy version 2.8.x
```

This confirms:

* Correct version
* Correct binary path
* No legacy HAProxy running

---

## 4. HAProxy Architecture Role

```
User
  ↓
Internet
  ↓
HAProxy :80 / :443   (SSL termination, redirects)
  ↓
NGINX :9070         (reverse proxy, static, app LB)
  ↓
Java Admin Cluster  (9071 / 9072 / 9073)
```

**Design principle**:

> HAProxy is an edge transport layer, not an application router.

---

## 5. HAProxy Configuration

### 5.1 Configuration File Location

```
/etc/haproxy/haproxy.cfg
```

---

### 5.2 Full Production Configuration

```haproxy
global
    log stdout format raw local0
    maxconn 50000
    daemon
    tune.ssl.default-dh-param 2048

defaults
    log global
    mode http

    option httplog
    option dontlognull
    option forwardfor

    timeout connect 5s
    timeout client  60s
    timeout server  60s

# ============================================================
# HTTP Frontend (80)
# Redirect all traffic to HTTPS
# ============================================================
frontend http_in
    bind *:80
    bind :::80
    redirect scheme https code 301 if !{ ssl_fc }

# ============================================================
# HTTPS Frontend (443)
# Internet-facing entry point
# ============================================================
frontend https_in
    bind *:443 ssl crt /etc/letsencrypt/live/admin.leocdp.com/fullchain.pem \
                     key /etc/letsencrypt/live/admin.leocdp.com/privkey.pem
    bind :::443 ssl crt /etc/letsencrypt/live/admin.leocdp.com/fullchain.pem \
                      key /etc/letsencrypt/live/admin.leocdp.com/privkey.pem

    http-request set-header X-Forwarded-Proto https
    http-request set-header X-Forwarded-Port 443

    default_backend nginx_admin

# ============================================================
# Backend → NGINX (Internal Reverse Proxy)
# NO Java load balancing here
# ============================================================
backend nginx_admin
    option httpchk GET /
    server nginx1 127.0.0.1:9070 check
```

---

## 6. Validate Configuration

Before restarting HAProxy, always validate:

```bash
sudo haproxy -c -f /etc/haproxy/haproxy.cfg
```

Expected result:

```
Configuration file is valid
```

If validation fails, **do not restart**.

---

## 7. Start & Enable HAProxy

```bash
sudo systemctl restart haproxy
sudo systemctl enable haproxy
```

Check status:

```bash
sudo systemctl status haproxy
```

---

## 8. Operational Notes (Important)

### 8.1 Why HAProxy Does Not Load Balance Java

* HAProxy has no knowledge of:

  * Application paths
  * Static vs API routes
  * Session affinity requirements
* NGINX already performs:

  * App-aware routing
  * Static serving
  * Java worker load balancing

**One layer = one responsibility.**

---

### 8.2 Scaling Strategy

* Add more Java nodes → update **NGINX only**
* Add rate limiting / WAF → HAProxy layer
* Add caching → NGINX layer
* Add CDN → in front of HAProxy

No architectural rewrites required.

---

## 9. Version Reference (for Documentation)

| Component  | Version         | Role                   |
| ---------- | --------------- | ---------------------- |
| HAProxy    | 2.8.x (LTS)     | Internet Load Balancer |
| NGINX      | 1.24.x (stable) | Internal Reverse Proxy |
| Java Admin | JDK-based       | Business Logic         |

---

## 10. Summary

* HAProxy 2.8 installed from **official maintained PPA**
* Acts as **secure Internet edge**
* Terminates SSL
* Forwards traffic cleanly to NGINX
* Keeps application complexity out of the edge
