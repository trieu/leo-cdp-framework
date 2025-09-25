Here’s a structured **README.md** draft for your **Airflow AI Agent** framework 👇

---

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
├── airflow-dags/           # DAG definitions (Redis-triggered, AI workflows, etc.)
├── airflow-output/         # Logs (webserver, scheduler, db-upgrade)
├── airflow-venv/           # Python virtual environment (auto-created)
├── requirements.txt        # Python dependencies
├── install-airflow.sh       # Script to install Airflow in a virtual environment
├── start-airflow.sh         # Script to start Airflow processes safely
├── stop-airflow.sh         # Script to stop Airflow processes safely
└── README.md               # You are here 🚀
```
---

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

We use **Redis Pub/Sub** to trigger DAGs externally.

### Example Publisher

```bash
redis-cli -p 6480 publish airflow-events "{dag_id:'redis_airflow_dag', params:'1234'}"
```

### Example Listener (Python)

check code at airflow-ai-agent/airflow-dags/redis_trigger.py



## 🔮 Roadmap

* [ ] Add **AI workflow DAG templates** (translation, summarization, personalization).
* [ ] Support **multi-tenant CDP events** with Airflow Variables.
* [ ] Add **Dockerized deployment** for production.
* [ ] Integrate ** PostgreSQL** for Airflow main database

---

## 👨‍💻 Contributing

PRs are welcome! Fork, branch, and open a PR.

---

## 📜 License

MIT — free to use, modify, and distribute.
