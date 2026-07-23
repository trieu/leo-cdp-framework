"""Runs one (or more) Customer Identity Resolution (CIR) batch(es) and prints
a summary of the resulting master profiles so you can verify the results
yourself (e.g. via psql) afterwards.

Intended to run after scripts/init_sample_data.py has seeded sample
AppsFlyer / MoEngage / Web Tracking raw profiles, but also works as a
general "process everything pending right now" utility.
"""

import logging
import os
import sys
from pathlib import Path

import psycopg2
from dotenv import load_dotenv
from psycopg2.extras import RealDictCursor

# Make the identity_resolution package importable when this script is run
# directly (python scripts/run_demo_resolution.py) rather than as a module.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from identity_resolution.resolver import CustomerIdentityResolver  # noqa: E402

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_NAME = os.environ.get("DB_NAME", "customer360")
DB_USER = os.environ.get("DB_USER", "postgres")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "password")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_SCHEMA = os.environ.get("DB_SCHEMA", "customer360")
BATCH_SIZE = int(os.environ.get("CIR_BATCH_SIZE", "5000"))

# Must match scripts/init_sample_data.py
DEMO_TENANT_ID = "11111111-1111-1111-1111-111111111111"


def _table(name: str) -> str:
    return f"{DB_SCHEMA}.{name}" if DB_SCHEMA else name


def _short_hash(value):
    """Truncates a SHA-256 hex digest for compact display (None passes through)."""
    if not value:
        return value
    return f"{value[:12]}..."


def print_summary(cursor) -> None:
    cursor.execute(
        f"""
        SELECT master_profile_id, domain, full_name, email, phone_number,
               national_id, device_ids, advertising_ids, cookie_ids,
               external_ids, source_systems, status_code, is_hashed, persona_name
        FROM {_table('cdp_master_profiles')}
        WHERE tenant_id = %s
        ORDER BY domain, created_at;
        """,
        (DEMO_TENANT_ID,),
    )
    masters = cursor.fetchall()

    print(f"\n=== {len(masters)} master profile(s) resolved for tenant {DEMO_TENANT_ID} ===")
    print("(full_name/email/phone_number/national_id are SHA-256 hashes -- no plaintext PII is stored)")
    print("(persona_name is the readable, non-PII stand-in required whenever is_hashed = TRUE)")
    for m in masters:
        print(
            f"- [{m['domain']}] {m['master_profile_id']} (status_code={m['status_code']})\n"
            f"    is_hashed={m['is_hashed']} persona_name={m['persona_name']!r}\n"
            f"    full_name={_short_hash(m['full_name'])!r} email={_short_hash(m['email'])!r} "
            f"phone={_short_hash(m['phone_number'])!r} national_id={_short_hash(m['national_id'])!r}\n"
            f"    device_ids={m['device_ids']} advertising_ids={m['advertising_ids']} "
            f"cookie_ids={m['cookie_ids']}\n"
            f"    external_ids={m['external_ids']} source_systems={m['source_systems']}"
        )

    cursor.execute(
        f"""
        SELECT status_code, COUNT(*) AS cnt
        FROM {_table('cdp_raw_profiles_stage')}
        WHERE tenant_id = %s
        GROUP BY status_code
        ORDER BY status_code;
        """,
        (DEMO_TENANT_ID,),
    )
    print("\n=== raw profile status_code counts (3 = processed) ===")
    for row in cursor.fetchall():
        print(f"- status_code={row['status_code']}: {row['cnt']}")


def main() -> None:
    conn = psycopg2.connect(
        host=DB_HOST, dbname=DB_NAME, user=DB_USER, password=DB_PASSWORD, port=DB_PORT
    )
    try:
        resolver = CustomerIdentityResolver(conn, schema=DB_SCHEMA, batch_size=BATCH_SIZE)
        total_processed = 0
        while True:
            processed = resolver.run_resolution_batch()
            total_processed += processed
            if processed < BATCH_SIZE:
                break
        logger.info("Resolution complete. Total raw profiles processed: %d", total_processed)

        with conn.cursor(cursor_factory=RealDictCursor) as cursor:
            print_summary(cursor)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
