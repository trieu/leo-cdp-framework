"""Docker HEALTHCHECK probe for the CIR worker container.

This service is a background worker with no HTTP surface, so the most
meaningful liveness signal is "can it still reach PostgreSQL" -- the same
connection every worker cycle (see daily_job.py) depends on.
"""

import os
import sys

import psycopg2

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_NAME = os.environ.get("DB_NAME", "customer360")
DB_USER = os.environ.get("DB_USER", "postgres")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "postgres")
DB_PORT = os.environ.get("DB_PORT", "5432")


def main() -> int:
    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            dbname=DB_NAME,
            user=DB_USER,
            password=DB_PASSWORD,
            port=DB_PORT,
            connect_timeout=3,
        )
        conn.close()
        return 0
    except Exception as exc:  # noqa: BLE001 - any failure means "unhealthy"
        print(f"healthcheck failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
