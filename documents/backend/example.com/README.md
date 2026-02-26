# LEO CDP — Local SSL Setup for `example.com` (Ubuntu + Nginx)

## Objective

Configure **locally trusted HTTPS** for LEO CDP development domains using:

* `mkcert` (local CA)
* Nginx reverse proxy
* Multiple LEO CDP services
* Chrome trusted certificates

This setup enables:

* Secure cookies (SSO / Keycloak)
* HTTPS-only APIs
* Same-domain subdomain testing
* Production-like local environment

---

# 1. Architecture Overview

Local domains used:

| Service       | Domain               | Port (Backend) |
| ------------- | -------------------- | -------------- |
| LEO CDP Admin | `leocdp.example.com` | 9070           |
| LEO SSO       | internal             | 9079           |
| ArangoDB      | proxied              | 8529           |
| Data Observer | `obs.example.com`    | 9080           |

Traffic flow:

```
Browser (HTTPS)
      ↓
    Nginx :443
      ↓
  Reverse Proxy
      ↓
LEO Services (localhost ports)
```

---

# 2. Install mkcert (Local Trusted CA)

`mkcert` creates certificates trusted by your OS and browser.

## Install dependencies

```bash
sudo apt update
sudo apt install libnss3-tools -y
```

---

## Install mkcert binary

```bash
wget https://github.com/FiloSottile/mkcert/releases/download/v1.4.4/mkcert-v1.4.4-linux-amd64

sudo cp mkcert-v1.4.4-linux-amd64 /usr/local/bin/mkcert
sudo chmod +x /usr/local/bin/mkcert
```

Verify:

```bash
mkcert -version
```

---

## Install Local CA

```bash
mkcert -install
```

This step:

* creates a local Certificate Authority
* registers it into system + browser trust store

---

## Check CA location

```bash
mkcert -CAROOT
```

---

# 3. Generate SSL Certificates

Generate certificate supporting multiple hosts:

```bash
mkcert example.com "*.example.com" example.test localhost 127.0.0.1 ::1
```

Generated files:

```
example.com+5.pem
example.com+5-key.pem
```

---

## Add certificate to Ubuntu trust store

```bash
sudo cp example.com+5.pem \
/usr/local/share/ca-certificates/example.com+5.crt

sudo update-ca-certificates
```

---

## Restart Chrome

Chrome caches certificate trust.

**Important:** fully close all Chrome processes.

```bash
pkill chrome
```

Reopen browser afterward.

---

# 4. Certificate Location

Example storage path:

```
/home/thomas/0-uspa/localhost-ssl/
```

Files:

```
example.com+5.pem
example.com+5-key.pem
```

---

# 5. Nginx Configuration — LEO CDP

---

## 5.1 Upstream Backends

```nginx
upstream backend_admin_cdp {
  server 127.0.0.1:9070;
}

upstream backend_sso_cdp {
  server 127.0.0.1:9079;
}

upstream backend_arangodb {
  server 127.0.0.1:8529;
}
```

---

## 5.2 LEO CDP Admin — HTTPS Server

```nginx
server {
  server_name leocdp.example.com;
```

---

### Static Admin UI (`/view/`)

```nginx
location ^~ /view/ {
    alias /home/thomas/0-github/leo-cdp-framework/core-leo-cdp/resources/app-templates/leocdp-admin/;

    try_files $uri $uri/ =404;

    add_header Cache-Control "public, max-age=2592000";
    expires 30d;

    gzip on;
    gzip_comp_level 6;
    gzip_min_length 1024;
    gzip_vary on;
    gzip_types text/css application/javascript application/x-javascript;
}
```

---

### SSO Routing

```nginx
location ^~ /_ssocdp/ {
    proxy_pass http://backend_admin_cdp;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_read_timeout 600s;
}
```

---

### ArangoDB Proxy

```nginx
location ^~ /_db/ {
    proxy_pass http://backend_arangodb;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_read_timeout 600s;
}
```

---

### Default Backend Proxy

```nginx
location / {
    proxy_pass http://backend_admin_cdp/;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Port $server_port;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-For "115.74.22.116";
    access_log off;
}
```

---

### SSL Configuration

```nginx
listen [::]:443 ssl http2;
listen 443 ssl http2;

ssl_certificate /home/thomas/0-uspa/localhost-ssl/example.com+5.pem;
ssl_certificate_key /home/thomas/0-uspa/localhost-ssl/example.com+5-key.pem;
}
```

---

# 6. LEO Data Observer — HTTPS

```nginx
server {
    server_name obs.example.com;

    location / {
        proxy_pass http://127.0.0.1:9080/;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Port $server_port;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For "115.74.22.116";

        expires 1M;
        access_log off;
        add_header Cache-Control "public";
    }

    listen [::]:443 ssl http2;
    listen 443 ssl http2;

    ssl_certificate /home/thomas/0-uspa/localhost-ssl/example.com+5.pem;
    ssl_certificate_key /home/thomas/0-uspa/localhost-ssl/example.com+5-key.pem;
}
```

---

## HTTP → HTTPS Redirect

```nginx
server {
    if ($host = obs.example.com) {
        return 301 https://$host$request_uri;
    }

    listen 80;
    listen [::]:80;
    server_name obs.example.com;

    return 404;
}
```

---

# 7. Enable Configuration

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

# 8. Hosts File Configuration

Add local DNS mapping:

```bash
sudo nano /etc/hosts
```

Add:

```
127.0.0.1 leocdp.example.com
127.0.0.1 obs.example.com
```

---

# 9. Validation Checklist

✅ Browser shows secure lock icon
✅ No SSL warning
✅ Cookies marked Secure work
✅ SSO redirects succeed
✅ HTTP/2 active
✅ Subdomains trusted

Test:

```
https://leocdp.example.com
https://obs.example.com
```

---

# 10. Common Issues

### Chrome still shows insecure

Fix:

```
Restart Chrome completely
```

---

### Certificate not trusted

Re-run:

```bash
mkcert -install
sudo update-ca-certificates
```

---

### Nginx fails reload

Check:

```bash
sudo nginx -t
```

---

# References

* [https://github.com/FiloSottile/mkcert](https://github.com/FiloSottile/mkcert)
* [https://kifarunix.com/create-locally-trusted-ssl-certificates-with-mkcert-on-ubuntu-20-04/](https://kifarunix.com/create-locally-trusted-ssl-certificates-with-mkcert-on-ubuntu-20-04/)
* [https://stackoverflow.com/questions/7580508/getting-chrome-to-accept-self-signed-localhost-certificate](https://stackoverflow.com/questions/7580508/getting-chrome-to-accept-self-signed-localhost-certificate)

---

