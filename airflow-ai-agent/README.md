# Airflow AI Agent Framework

🚀 **Airflow AI Agent** is a development framework that integrates **LEO CDP events** with **Apache Airflow DAGs** to orchestrate AI-driven pipelines.
It listens to customer and tracking events from LEO CDP, triggers Airflow DAGs, and runs **AI Agents** for personalization, enrichment, and automation.

---

## ✨ Features

* **Event-driven orchestration**: Trigger DAG runs via Redis Pub/Sub or manual `airflow dags trigger`.
* **Identity-aware AI Agents**: Connect with **PostgreSQL 16 + pgvector** for Customer 360° and vector search.
* **Multi-database support**: Works with **Postgres** (structured + embeddings) and **ArangoDB** (graph queries).
* **AI Integration**: Supports **Google Gemini (GenAI)**, **Google Translate API**, and **LangChain** for AI workflows.
* **Dev-friendly**: Includes scripts to set up Airflow 2.11 in a virtual environment, auto-create default admin user, and live DAG reload.

---

## 📂 Project Structure

```
airflow-ai-agent/
├── airflow-dags/             # DAG definitions (Redis-triggered, AI workflows, etc.)
├── airflow-output/           # Logs (webserver, scheduler, db-upgrade)
├── airflow-venv/             # Python virtual environment (auto-created)
├── requirements.txt          # Python dependencies
├── install-airflow.sh        # Script to install Airflow in a virtual environment
├── start-airflow.sh          # Script to start Airflow processes safely
├── stop-airflow.sh           # Script to stop Airflow processes safely
├── trigger-ai-agent-jobs.sh  # Script to start or stop AI Agent trigger jobs using Redis PubSub
└── README.md                 # You are here 🚀
```
---

## ⚙️ Environment

* Create .env file for Airflow
* Run and edit your .env file 

```bash
cp sample.env.txt .env
```

## ⚙️ Installation

```bash
cd airflow-ai-agent
./install-airflow.sh
```

## 🛠 Development Setup

### Start Airflow

```bash
./start-airflow.sh
```

* Uses **current folder as `AIRFLOW_HOME`**
* Auto-creates `Admin` user (`admin / leocdp123`) if `DEV_MODE=true`
* Webserver → [http://localhost:8080](http://localhost:8080)

### Stop Airflow

```bash
./stop-airflow.sh
```

* Gracefully stops webserver + scheduler.

---

## 📡 Event-Driven Triggers

The **Airflow AI Agent** can run DAGs automatically when events are published to **Redis Pub/Sub**.
This allows **LEO CDP** or any external service to notify Airflow when an AI workflow should start.

---

### Example Publisher (Redis CLI)

Publish an event to trigger a DAG run:

```bash
# Trigger a simple test DAG
redis-cli -p 6480 publish ai-agent-events '{"dag_id":"redis_airflow_dag", "params":{"run_id":"test123"}}'

# Trigger an AI Agent DAG to process content keywords for a profile
redis-cli -p 6480 publish ai-agent-events '{"dag_id":"leo_aia_content_keywords", "params":{"profile_id":"p123"}}'

# Trigger another DAG with multiple params
redis-cli -p 6480 publish ai-agent-events '{"dag_id":"leo_aia_translate_text", "params":{"profile_id":"p999","lang":"vi"}}'
```

---

### Example Listener (Python)

The listener subscribes to the `ai-agent-events` channel and triggers DAGs dynamically based on the message payload.

👉 Full code is available in
`airflow-ai-agent/airflow-dags/ai_agent_trigger_leocdp.py`

```bash 
./trigger-ai-agent-jobs.sh start
```

---

### ✅ Supported Payload Format

Messages published to Redis should be valid JSON-like strings with the following fields:

```json
{
  "dag_id": "your_dag_id_here",
  "params": {
    "key": "value",
    "profile_id": "p123"
  }
}
```

* `dag_id` → the Airflow DAG to run
* `params` → custom parameters passed into `--conf`

---

⚡ This makes Airflow reactive: whenever **LEO CDP** detects a new event (like a profile update, translation request, or content processing job), it simply publishes a message to Redis, and the Airflow AI Agent takes care of running the right DAG.

---

## 🔮 Roadmap

The following enhancements are planned to make **Airflow AI Agent** more powerful, production-ready, and developer-friendly:

* [ ] **Prebuilt AI workflow DAG templates**

  * Translation (multi-language content pipelines)
  * Summarization (long text → short insights)
  * Personalization (profile-based recommendations)
  * Keyword extraction + embedding storage in `pgvector`

* [ ] **Multi-tenant event handling**

  * Pass `tenant_id` from **LEO CDP events** into Airflow DAGs
  * Use **Airflow Variables / Connections** to isolate tenant configs
  * Example: one Redis channel → multiple isolated workflows per tenant

* [ ] **Production-ready deployment**

  * Official **Docker Compose** + **Kubernetes Helm Chart**
  * Configurable `AIRFLOW_HOME`, DAG folders, secrets, and Redis connection
  * Example: deploy with `docker compose up airflow`

* [ ] **Database Integration**

  * Use **PostgreSQL 16** as the Airflow **metadata DB**
  * Store embeddings in **pgvector** for semantic search
  * Event-driven enrichment pipelines writing back to `cdp_master_profiles`

* [ ] **Monitoring & Observability**

  * Airflow **metrics in Prometheus/Grafana**
  * Auto-log Redis event payloads into PostgreSQL
  * Example dashboards: DAG latency, Redis events processed per second

* [ ] **Developer Experience**

  * Hot-reload DAGs during local dev (`airflow dags reload`)
  * Example notebooks for testing DAG logic outside of Airflow
  * Pre-configured `dev.env` for one-line bootstrap

---

## 🚀 Future AI Agent Use Cases

These are forward-looking scenarios that extend Airflow beyond orchestration into **intelligent automation**:

* [ ] **RAG with Gemini + pgvector**

  * Orchestrate Retrieval-Augmented Generation (RAG) workflows
  * Store embeddings in PostgreSQL `pgvector`
  * Example: customer queries → semantic search → Gemini API → response

* [ ] **Auto-segmentation in LEO CDP**

  * Run scheduled clustering jobs on customer profiles
  * Auto-generate dynamic customer segments (e.g., high-value travelers, churn risk)
  * Push segments back to Redis for real-time personalization

* [ ] **Email Marketing Pipelines**

  * DAGs for generating personalized newsletters with AI templates
  * Connect with CDP + CRM for recipient targeting
  * Multi-variant (A/B/n) content generation and automatic performance tracking

* [ ] **Multi-Agent Orchestration**

  * Trigger multiple AI agents (Summarizer, Recommender, Translator) in one DAG
  * Pass intermediate outputs through Redis / PostgreSQL
  * Example: blog ingestion → summarization → translation → email campaign

* [ ] **Travel & E-commerce Personalization**

  * Enrich user sessions with CDP data + AI-driven itinerary recommendations
  * Event-driven product recommendation workflows
  * Example: Redis event “user searches Paris” → Airflow DAG → personalized travel plan → push to app

---

## 👨‍💻 Contributing

PRs are welcome! Fork, branch, and open a PR.

---

## 📜 License

MIT — free to use, modify, and distribute.
