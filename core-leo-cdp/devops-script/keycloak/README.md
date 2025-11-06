# 🦾 Keycloak Docker Launcher

This repository provides a simple, configurable shell script (`run-keycloak.sh`) for running **Keycloak** in Docker with HTTPS reverse-proxy support via Nginx.
It’s designed for local or staging setups where you want **Keycloak reachable over HTTPS** (e.g. `https://leoid.example.com`) without managing Keycloak’s own TLS directly.

---

## 🚀 Features

* Runs Keycloak in Docker with one command
* Loads environment variables from a `.env` file
* Safe restarts — automatically stops and removes old containers
* Colorized terminal output for clarity
* Real-time Keycloak logs after launch
* Compatible with **Nginx reverse proxy + HTTPS termination**
* Easily extended for production use with persistent data

---

## ⚙️ Requirements

* **Docker** installed and running
* **Nginx** (or another reverse proxy) handling HTTPS on port 443
* Hostname (e.g. `leoid.example.com`) mapped to `127.0.0.1` in `/etc/hosts`
* Linux or macOS shell (tested on Ubuntu and Fedora)

---

## 📁 Project structure

```
keycloak/
│
├── run-keycloak.sh      # Main launcher script
├── .env                 # Configuration (environment variables)
└── README.md            # You are here
```

---

## 🔧 Setup

1. **Clone this repo or copy the files**

   ```bash
   mkdir ~/keycloak && cd ~/keycloak
   ```

2. **Create the `.env` file**

   Example:

   ```bash
      # Keycloak settings
      KEYCLOAK_VERSION=26.4.2
      KEYCLOAK_PORT=8080

      # for local dev only
      KC_BOOTSTRAP_ADMIN_USERNAME=admin
      KC_BOOTSTRAP_ADMIN_PASSWORD=admin
      KC_PROXY=edge
      KC_HOSTNAME_STRICT=false
      KC_HOSTNAME_STRICT_HTTPS=false
      KC_HTTP_ENABLED=true
      KC_HTTP_RELATIVE_PATH=/
      KC_HOSTNAME_URL=http://leoid.example.com
      KC_PROXY_HEADERS=xforwarded

   ```

   These variables control the container startup and Keycloak’s internal HTTPS configuration.

3. **Make the script executable**

   ```bash
   chmod +x run-keycloak.sh
   ```

4. **Run Keycloak**

   ```bash
   ./run-keycloak.sh
   ```

   The script will:

   * Stop and remove any old container named `keycloak`
   * Start a new one with your settings
   * Show live logs immediately

---

## 🌐 Access Keycloak

Once running, visit:

```
https://leoid.example.com
```

Default credentials (from `.env`):

```
Username: admin
Password: admin
```

> Note: The `KC_PROXY=edge` and `KC_PROXY_HEADERS=xforwarded` settings tell Keycloak that HTTPS is handled by Nginx — not by the container itself.

---

## 🧱 Nginx example

Make sure your Nginx block for `leoid.example.com` looks like this:

```nginx
server {
    listen 443 ssl http2;
    server_name leoid.example.com;

    ssl_certificate /path/to/example.com.pem;
    ssl_certificate_key /path/to/example.com-key.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Port 443;
    }
}
```

---

## 🧩 Tips

* To check running container:

  ```bash
  docker ps
  ```
* To view logs anytime:

  ```bash
  docker logs -f keycloak
  ```
* To stop Keycloak:

  ```bash
  docker stop keycloak
  ```

---

## 🔮 Future improvements

* Add persistent storage for realm/user data (`docker volume`)
* Integrate PostgreSQL
* Use Docker Compose for one-command orchestration
* Add auto-restart policy (`--restart unless-stopped`)

---

## 🧠 Summary

| Task           | Command                   |
| -------------- | ------------------------- |
| Start Keycloak | `./run-keycloak.sh`       |
| View logs      | `docker logs -f keycloak` |
| Stop Keycloak  | `docker stop keycloak`    |
| Edit settings  | `nano .env`               |

Keycloak will be available at: **`https://leoid.example.com`**

---

Would you like me to extend this README with **a persistent Postgres + Keycloak `docker-compose.yml` example** next (so realm data and users survive restarts)?
