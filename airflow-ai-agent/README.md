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

The **Airflow AI Agent** can run DAGs automatically when events are published to **Redis Pub/Sub**.
This allows **LEO CDP** or any external service to notify Airflow when an AI workflow should start.

---

### Example Publisher (Redis CLI)

Publish an event to trigger a DAG run:

```bash
# Trigger a simple test DAG
redis-cli -p 6480 publish airflow-events "{dag_id:'redis_airflow_dag', params:{'run_id':'test123'}}"

# Trigger an AI Agent DAG to process content keywords for a profile
redis-cli -p 6480 publish airflow-events "{dag_id:'leo_aia_content_keywords', params:{'profile_id':'p123'}}"

# Trigger another DAG with multiple params
redis-cli -p 6480 publish airflow-events "{dag_id:'leo_aia_translate_text', params:{'profile_id':'p999','lang':'vi'}}"
```

---

### Example Listener (Python)

The listener subscribes to the `airflow-events` channel and triggers DAGs dynamically based on the message payload.

👉 Full code is available in
`airflow-ai-agent/airflow-dags/redis_trigger.py`

```python
import redis, subprocess, json

def trigger_airflow_dag(dag_id, params):
    print(f"🚀 Triggering DAG: {dag_id} with params={params}")
    subprocess.run([
        "airflow", "dags", "trigger",
        dag_id,
        "--conf", json.dumps(params)
    ])

def main():
    r = redis.Redis(host="localhost", port=6480, db=0)
    pubsub = r.pubsub()
    pubsub.subscribe("airflow-events")

    print("📡 Listening to Redis channel: airflow-events")
    for message in pubsub.listen():
        if message["type"] == "message":
            data = message["data"].decode("utf-8")
            try:
                payload = json.loads(data.replace("'", '"'))  # handle single quotes
                dag_id = payload.get("dag_id")
                params = payload.get("params", {})
                trigger_airflow_dag(dag_id, params)
            except Exception as e:
                print(f"❌ Invalid payload: {data} ({e})")

if __name__ == "__main__":
    main()
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
