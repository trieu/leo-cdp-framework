import redis
import subprocess
import json
from redis_connection import connect_to_redis

def trigger_airflow_dag(dag_id: str, params: dict):
    print(f"Triggering Airflow DAG: {dag_id} with params: {params}")
    conf_str = json.dumps(params)  # Airflow requires JSON string
    subprocess.run([
        "airflow", "dags", "trigger", dag_id,
        "--conf", conf_str
    ])

def main():
    r = connect_to_redis()
    pubsub = r.pubsub()
    pubsub.subscribe("airflow-events")  # channel name

    print("Listening to Redis channel: airflow-events")
    for message in pubsub.listen():
        if message["type"] == "message":
            raw_data = message["data"].decode("utf-8")
            print(f"Received: {raw_data}")

            try:
                payload = json.loads(raw_data)
                dag_id = payload.get("dag_id", "redis_airflow_dag")
                params = payload.get("params", {})

                # If params is not dict (e.g., "1234"), wrap it
                if not isinstance(params, dict):
                    params = {"param": str(params)}

                trigger_airflow_dag(dag_id, params)

            except json.JSONDecodeError:
                print("⚠️ Invalid JSON message, ignoring")

if __name__ == "__main__":
    main()

# to test, send :
# redis-cli -p 6480 publish airflow-events '{"dag_id":"redis_airflow_dag","params":{"foo":"1234"}}'