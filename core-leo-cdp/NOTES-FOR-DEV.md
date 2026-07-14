
# LeoCDP Development Setup

This guide explains how to prepare a local development environment for LeoCDP using Docker.

## Prerequisites

- Docker Engine 24+
- Docker Compose v2
- Java 11+
- Git

---

## Services

The development environment includes:

| Service | Version | Host | Port |
|---------|----------|------|------|
| ArangoDB | 3.11.14 | leocdp.database | 8529 |
| Redis | 8 | leocdp.redis | 6480 |

---

## Start the services

```bash
docker compose up -d
```

Verify:

```bash
docker ps
```

---

## Configure local hostname resolution

If LeoCDP is running directly on your local machine (not inside Docker), add the following entries to your hosts file.

Linux/macOS:

```bash
sudo nano /etc/hosts
```

Windows:

```
C:\Windows\System32\drivers\etc\hosts
```

Append:

```text
# hosts for leocdp dev
127.0.0.1   leocdp.database
127.0.0.1   leocdp.redis
127.0.0.1   leocdp.example.com obs.example.com
127.0.0.1   cdpsys.admin cdpsys.observer
```

Verify:

```bash
ping leocdp.database
ping leocdp.redis
```

---

## ArangoDB

Open:

```
http://localhost:8529
```

Credentials:

```
Username: root
Password: 123456
```

---

## Create the LeoCDP database

Open a shell inside the container:

```bash
docker exec -it ArangoDB311 arangosh \
    --server.username root \
    --server.password 123456
```

Create the database:

```javascript
db._createDatabase("leo_cdp_test")
```

Verify:

```javascript
db._databases()
```

Expected:

```
[
  "_system",
  "leo_cdp_test"
]
```

---

## Redis

Test the Redis connection:

```bash
redis-cli -h 127.0.0.1 -p 6480 ping
```

Expected:

```
PONG
```

---

## LeoCDP Configuration

- Create file: core-leo-cdp/configs/DEV-database-configs.json
- Configure LeoCDP with the following database settings:

```json
{
  "configs": {
    "localCdpDbConfigs": {
      "username": "root",
      "password": "123456",
      "database": "leo_cdp_test",
      "host": "leocdp.database",
      "port": 8529,
      "dbdriver": "arangodb",
      "dbdriverclasspath": "com.arangodb.ArangoDB",
      "enabled": true,
      "partitionMap": {
        "Profile": 3
      }
    }
  }
}
```

Redis endpoint:

```
Host: leocdp.redis
Port: 6480
```

---

## Stop the services

```bash
docker compose down
```

Remove containers and volumes:

```bash
docker compose down -v
```

---

## Persistent Storage

Data is stored under:

```
data/
├── arangodb/
├── arangodb-apps/
└── redis/
```

Deleting these directories will reset the local development environment.

---

## Default Development Endpoints

| Service | URL |
|---------|-----|
| ArangoDB Web UI | http://localhost:8529 |
| ArangoDB API | http://leocdp.database:8529 |
| Redis | leocdp.redis:6480 |

---

## Java Development Environment

LeoCDP is built with the following runtime:

| Component  | Version                          |
| ---------- | -------------------------------- |
| Java       | 11                               |
| Gradle     | 6.9.4                            |
| Build Tool | Gradle                           |
| IDE        | Visual Studio Code (Recommended) |

> **Important**
>
> LeoCDP requires **Gradle 6.9.4**. Newer Gradle versions (7.x or 8.x) are not supported for this project.

---
## Install Gradle 6.9.4 (Ubuntu/Linux)

### Step 1. Download Gradle

```bash
cd ~/Downloads

wget https://services.gradle.org/distributions/gradle-6.9.4-bin.zip
```

Or download it using your web browser:

https://services.gradle.org/distributions/gradle-6.9.4-bin.zip

Verify the file exists:

```bash
ls -lh gradle-6.9.4-bin.zip
```

---

### Step 2. Create the installation directory

```bash
sudo mkdir -p /opt/gradle
```

---

### Step 3. Install the `unzip` utility (if needed)

```bash
sudo apt update
sudo apt install unzip -y
```

---

### Step 4. Extract Gradle

```bash
sudo unzip gradle-6.9.4-bin.zip -d /opt/gradle
```

The directory structure should become:

```
/opt/gradle/
└── gradle-6.9.4/
    ├── bin/
    ├── lib/
    ├── init.d/
    └── ...
```

Verify:

```bash
ls /opt/gradle
```

Expected output:

```
gradle-6.9.4
```

---

### Step 5. Configure environment variables

Create a Gradle environment configuration:

```bash
sudo nano /etc/profile.d/gradle.sh
```

Paste:

```bash
export GRADLE_HOME=/opt/gradle/gradle-6.9.4
export PATH=$GRADLE_HOME/bin:$PATH
```

Save and exit.

Make it executable:

```bash
sudo chmod +x /etc/profile.d/gradle.sh
```

Load the configuration:

```bash
source /etc/profile.d/gradle.sh
```

Alternatively, simply log out and log back in.

---

### Step 6. Verify the installation

Check Gradle:

```bash
gradle --version
```

Expected output:

```
------------------------------------------------------------
Gradle 6.9.4
------------------------------------------------------------

Build time: ...
Kotlin: ...
Groovy: ...
JVM: 11.x
OS: Linux
```

Verify the executable location:

```bash
which gradle
```

Expected:

```
/opt/gradle/gradle-6.9.4/bin/gradle
```

---

## Configure Visual Studio Code

Create or edit:

```
.vscode/settings.json
```

```json
{
    "java.import.gradle.enabled": true,
    "java.import.gradle.home": "/opt/gradle/gradle-6.9.4"
}
```

Reload VS Code.

Open the Command Palette (`Ctrl+Shift+P`) and run:

```
Java: Clean Java Language Server Workspace
```

Then reopen the LeoCDP project.

---

## Verify VS Code

Open the integrated terminal and run:

```bash
gradle --version
```

The version should be:

```
Gradle 6.9.4
```

VS Code should now import the project using the Gradle installation located at:

```
/opt/gradle/gradle-6.9.4
```
