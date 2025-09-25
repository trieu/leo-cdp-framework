import json
import logging
import subprocess
import signal
import sys
import time
from typing import Any, Dict

import redis
import requests
from redis_connection import connect_to_redis
from dotenv import load_dotenv
import os

# --- Load .env ---
load_dotenv(override=True)

AIRFLOW_WEB_SERVER_HOST = os.getenv("AIRFLOW_WEB_SERVER_HOST", "0.0.0.0")
AIRFLOW_WEB_SERVER_PORT = os.getenv("AIRFLOW_WEB_SERVER_PORT", "8080")
AIRFLOW_BASE_URL = os.getenv("AIRFLOW_BASE_URL", f"http://{AIRFLOW_WEB_SERVER_HOST}:{AIRFLOW_WEB_SERVER_PORT}")
AIRFLOW_USERNAME = os.getenv("AUTH_USER", "")
AIRFLOW_PASSWORD = os.getenv("AUTH_PASSWORD", "")
REDIS_CHANNEL = os.getenv("REDIS_CHANNEL", "ai-agent-events")
USE_API = os.getenv("USE_API", "true").lower() == "true"
LOG_FILE = "./airflow-output/ai_agent_trigger_code.log"

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[
        logging.FileHandler(LOG_FILE),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger(__name__)

shutdown_flag = False


def handle_signal(sig, frame):
    global shutdown_flag
    logger.info(f"Received signal {sig}, shutting down gracefully...")
    shutdown_flag = True


# Register signal handlers
signal.signal(signal.SIGINT, handle_signal)
signal.signal(signal.SIGTERM, handle_signal)


def trigger_airflow_dag_cli(dag_id: str, params: Dict[str, Any]) -> bool:
    """Trigger an Airflow DAG using the CLI."""
    conf_str = json.dumps(params)
    try:
        subprocess.run(
            ["airflow", "dags", "trigger", dag_id, "--conf", conf_str],
            check=True
        )
        logger.info(f"Triggered DAG via CLI: {dag_id} with {params}")
        return True
    except subprocess.CalledProcessError as e:
        logger.error(f"CLI trigger failed for DAG {dag_id}: {e}")
        return False


def trigger_airflow_dag_api(dag_id: str, params: Dict[str, Any]) -> bool:
    """Trigger an Airflow DAG using the REST API (Airflow >= 2.0)."""
    url = f"{AIRFLOW_BASE_URL}/api/v1/dags/{dag_id}/dagRuns"
    headers = {"Content-Type": "application/json"}
    data = {"conf": params}

    try:
        response = requests.post(
            url, json=data, headers=headers,
            auth=(AIRFLOW_USERNAME, AIRFLOW_PASSWORD)
        )
        if response.status_code in (200, 201):
            logger.info(f"Triggered DAG via API: {dag_id} with {params}")
            return True
        else:
            logger.error(f"API error {response.status_code}: {response.text}")
            return False
    except Exception as e:
        logger.exception(f"API request failed for DAG {dag_id}: {e}")
        return False


def process_message(raw_data: str):
    """Process incoming Redis event and trigger Airflow DAG."""
    try:
        payload = json.loads(raw_data)
        dag_id = payload.get("dag_id", "redis_airflow_dag")
        params = payload.get("params", {})

        if not isinstance(params, dict):
            params = {"param": str(params)}

        logger.info(f"Received event for DAG: {dag_id}, params={params}")

        if USE_API:
            trigger_airflow_dag_api(dag_id, params)
        else:
            trigger_airflow_dag_cli(dag_id, params)

    except json.JSONDecodeError:
        logger.warning(f"⚠️ Invalid JSON message ignored: {raw_data}")


def main():
    """Main loop with auto-reconnect to Redis."""
    global shutdown_flag

    while not shutdown_flag:
        try:
            r = connect_to_redis()
            pubsub = r.pubsub()
            pubsub.subscribe(REDIS_CHANNEL)
            logger.info(f"Listening to Redis channel: {REDIS_CHANNEL}")

            for message in pubsub.listen():
                if shutdown_flag:
                    break
                if message["type"] == "message":
                    process_message(message["data"])

        except redis.exceptions.ConnectionError as e:
            logger.error(f"Redis connection error: {e}, retrying in 5s...")
            time.sleep(5)
        except Exception as e:
            logger.exception(f"Unexpected error: {e}, retrying in 5s...")
            time.sleep(5)

    logger.info("Shutdown complete.")


if __name__ == "__main__":
    main()


# to test, send :
# redis-cli -p 6480 publish ai-agent-events '{"dag_id":"redis_airflow_dag","params":{"foo":"1234"}}'