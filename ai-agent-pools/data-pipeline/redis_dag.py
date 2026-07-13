from datetime import datetime

from dagster import (
    op,
    job,
    OpExecutionContext,
)

from redis_connection import connect_to_redis


@op
def set_redis_key(context: OpExecutionContext):
    # Dagster run config (equivalent to Airflow dag_run.conf)
    run_config = context.run_config
    context.log.info(f"set_redis_key run_config: {run_config}")

    r = connect_to_redis()

    now = datetime.now()
    date_str = now.strftime("%Y%m%d")
    datetime_str = now.strftime("%Y-%m-%d %H:%M:%S")

    key_name = f"key_{date_str}"
    value = f"datetime_{datetime_str}"

    r.set(key_name, value)

    context.log.info(f"Key set in Redis: {key_name} = {value}")

    # Pass key name to downstream op
    return key_name


@op
def get_redis_key(context: OpExecutionContext, key_name: str):
    run_config = context.run_config
    context.log.info(f"get_redis_key run_config: {run_config}")

    r = connect_to_redis()

    value = r.get(key_name)

    if value:
        if isinstance(value, bytes):
            value = value.decode("utf-8")

        context.log.info(f"Key fetched from Redis: {key_name} = {value}")
    else:
        context.log.info(f"Key not found in Redis: {key_name}")


@job
def redis_job():
    get_redis_key(set_redis_key())