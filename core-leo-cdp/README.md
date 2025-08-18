# LEO CDP – Core System

<div style="background-color: #F0F8FF; text-align:center; border-radius:8px;">
	<img src="https://gcore.jsdelivr.net/gh/trieu/leo-cdp-framework@latest/core-leo-cdp/ai-first-customer360.png" alt="LEO CDP framework" >
</div>

**LEO CDP (Customer Data Platform)** is a modular, extensible platform for real-time **customer data unification, segmentation, and activation**.
This repository (`LeoTech-Core-System`) contains the **core engine** for event tracking, identity resolution, marketing automation, and analytics.

---

## 📂 Project Structure

```
core-leo-cdp/
├── bin/                        # Compiled binaries / scripts
├── configs/                    # Environment-specific configurations
│   ├── database-configs.json   # Default DB config
│   └── PRO-database-configs.json (gitignored) 
├── devops-script/              # Deployment / automation scripts
├── ext-lib/                    # External libraries / dependencies
├── public/                     # Public assets (if applicable)
├── resources/                  # Static resources & templates
│   ├── app-templates/          # Application templates
│   ├── content-templates/      # Marketing content templates
│   ├── database/               # Database initialization scripts
│   ├── data-for-new-setup/     # Sample bootstrap data
│   └── marketing-flow-templates/ # Marketing automation workflows
├── src/
│   ├── main/java/              # Core Java source code
│   └── test/java/              # Unit & integration tests
├── build.gradle                # Gradle build configuration
├── build.sh                    # Shell script for local build
├── leocdp-build.properties     # Build metadata
├── leocdp-metadata.properties  # CDP metadata definitions
├── NOTES-*.md                  # Developer and upgrade notes
└── README.md                   # This file
```

---

## 🚀 Key Features

* **Event Tracking**
  Collects web & app events (page views, clicks, transactions, etc.) with attribution support.

* **Identity Resolution**
  Resolves customer identities across multiple channels into unified master profiles.

* **Segmentation & Scoring**
  Supports RFM, CLV, lead scoring, and persona-based segmentation.

* **Personalization Engine**
  Powers content recommendations and real-time marketing workflows.

* **Extensible Templates**
  Includes ready-to-use templates for apps, content, and marketing flows.

* **DevOps Ready**
  Scripts for automated deployment, upgrades, and environment setup.

---

## ⚙️ Getting Started

### Prerequisites

* **Java 11+**
* **Gradle** (or use `build.sh`)
* **ArangoDB 3.11** (default CDP database for LEO CDP version 1.0 since 2020)
* **Kafka/Redis** (for event streaming and caching – optional but recommended)

### Build & Run

```bash
# Clone repo
git clone https://github.com/your-org/leo-cdp-core.git
cd leo-cdp-core

# Build
./build.sh

# Or use Gradle
./gradlew build
```

### Configurations

* Default configs are under `configs/database-configs.json`
* **Production configs** (e.g., `PRO-database-configs.json`) are **gitignored** – maintain your own copy locally.

---

## 🧑‍💻 Development Notes

* See `NOTES-FOR-DEV.md` for developer setup.
* See `NOTES-FOR-NEW-SETUP.md` for initializing a new environment.
* See `NOTES-FOR-UPGRADE.md` for migration steps.

---

## 📊 Roadmap

* [ ] From LEO CDP version 2.0, the main database is PostgreSQL 16+
* [ ] Support multi-tenant deployments
* [ ] Expand AI-powered segmentation & recommendation

---

# 🧑‍💻 Author & License

Created by: [Trieu Nguyen (Thomas)](https://www.facebook.com/dataism.one)  
**License**: Open Source - MIT-style.  
Use freely. Customize. Brand your own white-label CDP. Just respect the original creator 🙏.
