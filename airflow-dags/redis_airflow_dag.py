from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.utils.dates import days_ago
from datetime import datetime
from redis_connection import connect_to_redis

default_args = {
    "owner": "airflow",
    "depends_on_past": False,
    "start_date": days_ago(1),
    "retries": 1,
}

def set_redis_key(**kwargs):
    dag_conf = kwargs["dag_run"].conf
    print(f"set_redis_key DAG conf: {dag_conf}")
    
    r = connect_to_redis()
    now = datetime.now()
    date_str = now.strftime("%Y%m%d")      # e.g., 20250925
    datetime_str = now.strftime("%Y-%m-%d %H:%M:%S")
    key_name = f"key_{date_str}"
    value = f"datetime_{datetime_str}"
    r.set(key_name, value)
    print(f"Key set in Redis: {key_name} = {value}")

def get_redis_key(**kwargs):
    dag_conf = kwargs["dag_run"].conf
    print(f"get_redis_key DAG conf: {dag_conf}")
    
    r = connect_to_redis()
    now = datetime.now()
    date_str = now.strftime("%Y%m%d")
    key_name = f"key_{date_str}"
    value = r.get(key_name)
    if value:
        print(f"Key fetched from Redis: {key_name} = {value}")
    else:
        print(f"Key not found in Redis: {key_name}")


with DAG(
    dag_id="redis_airflow_dag",
    default_args=default_args,
    schedule=None,   # ✅ manual trigger only
    catchup=False,
) as dag:
    set_key_task = PythonOperator(
        task_id="set_redis_key",
        python_callable=set_redis_key,
    )

    get_key_task = PythonOperator(
        task_id="get_redis_key",
        python_callable=get_redis_key,
    )

set_key_task >> get_key_task
