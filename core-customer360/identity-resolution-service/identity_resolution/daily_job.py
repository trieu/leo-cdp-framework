"""Daily scheduled entry point for Customer Identity Resolution.

Runs standalone (cron, Airflow PythonOperator/@task, Dagster asset, etc.) and
fully drains the ``cdp_raw_profiles_stage`` staging table in successive
batches by repeatedly calling ``CustomerIdentityResolver.run_resolution_batch()``.
"""

import logging
import os
from datetime import datetime

import psycopg2
from dotenv import load_dotenv

from identity_resolution.resolver import CustomerIdentityResolver

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_NAME = os.environ.get("DB_NAME", "cdp")
DB_USER = os.environ.get("DB_USER", "postgres")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "postgres")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_SCHEMA = os.environ.get("DB_SCHEMA", "public")
BATCH_SIZE = int(os.environ.get("CIR_BATCH_SIZE", "5000"))


def run_daily_identity_resolution() -> int:
    """Connects to Postgres and drains the staging table until empty.

    Returns:
        The total number of raw profiles processed across all batches.
    """
    conn = psycopg2.connect(
        host=DB_HOST, dbname=DB_NAME, user=DB_USER, password=DB_PASSWORD, port=DB_PORT
    )
    total_processed = 0
    try:
        resolver = CustomerIdentityResolver(conn, schema=DB_SCHEMA, batch_size=BATCH_SIZE)
        logger.info("[%s] Starting daily identity resolution run.", datetime.now())

        while True:
            processed = resolver.run_resolution_batch()
            total_processed += processed
            if processed < BATCH_SIZE:
                break

        logger.info(
            "[%s] Daily run complete. Total profiles processed: %d",
            datetime.now(),
            total_processed,
        )
    finally:
        conn.close()

    return total_processed


if __name__ == "__main__":
    run_daily_identity_resolution()
