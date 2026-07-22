"""Seeds sample test data for a Customer Identity Resolution (CIR) demo run.

Connects to the PostgreSQL database configured via environment variables /
``.env`` (see ``daily_job.py``) and:

1. Ensures the ``pg_trgm`` / ``fuzzystrmatch`` extensions are enabled (needed
   for the ``fuzzy_dmetaphone`` matching rule used by other attributes).
2. Ensures the ``cdp_profile_attributes`` (matching-rule metadata) and
   ``cdp_id_resolution_status`` (real-time throttle state) tables exist --
   these are CIR runtime tables, not part of ``core-customer360/database-schema.sql``.
3. Seeds a set of active identity-resolution matching rules.
4. Clears any previous demo data (scoped to ``DEMO_TENANT_ID`` only, so it
   never touches other tenants) and inserts sample raw profiles simulating
   AppsFlyer (mobile attribution), MoEngage (engagement) and Web Tracking
   (GA4-style) events for both the banking and retail domains.

No Personal Data (PII) is ever written to the database: ``full_name``,
``email``, ``phone_number`` and ``national_id`` are one-way SHA-256 hashed
(normalized/trimmed/lowercased first) before insertion -- the same pattern
used by real-world hashed-match integrations (e.g. Meta/Google Customer
Match). Matching still works because identical inputs always hash to the
same value; only the ``full_name`` rule switches from fuzzy to exact
matching since hashes can't be fuzzy-compared.

Safe to re-run: every step is idempotent / scoped to ``DEMO_TENANT_ID``.
"""

import hashlib
import logging
import os

import psycopg2
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_NAME = os.environ.get("DB_NAME", "customer360")
DB_USER = os.environ.get("DB_USER", "postgres")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "password")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_SCHEMA = os.environ.get("DB_SCHEMA", "customer360")

# Fixed tenant used for the demo so this script is safe to re-run: only rows
# belonging to this tenant are ever deleted/re-seeded.
DEMO_TENANT_ID = "11111111-1111-1111-1111-111111111111"

# Raw-stage/master-profile columns that hold Personal Data (PII). Values for
# these columns are SHA-256 hashed before ever being written to the
# database -- see hash_pii() below.
HASHED_PII_FIELDS = ("full_name", "email", "phone_number", "national_id")

MATCHING_RULES = [
    # (attribute_internal_code, name, is_identity_resolution, matching_rule, matching_threshold, consolidation_rule)
    ("email", "Email (SHA-256 hashed)", True, "exact", None, "non_null"),
    ("phone_number", "Phone Number (SHA-256 hashed)", True, "exact", None, "non_null"),
    ("national_id", "National ID / KYC (SHA-256 hashed)", True, "exact", None, "non_null"),
    ("device_id", "Device ID", True, "exact", None, "non_null"),
    ("advertising_id", "Advertising ID (IDFA/GAID)", True, "exact", None, "non_null"),
    ("cookie_id", "Web Cookie ID", True, "exact", None, "non_null"),
    ("external_customer_id", "External Customer ID", True, "exact", None, "non_null"),
    # Hashed values can only ever be compared for exact equality (fuzzy trigram
    # similarity is meaningless on hash digests), so full_name uses 'exact' here.
    ("full_name", "Full Name (SHA-256 hashed)", True, "exact", None, "most_recent"),
]


def hash_pii(value):
    """Returns a SHA-256 hex digest of a normalized PII value, or None.

    Normalizes (trim + lowercase) before hashing so equivalent raw values
    (e.g. the same email reported by two different source systems) always
    collide to the same hash, preserving identity-resolution matching
    without ever storing the plaintext PII.
    """
    if value is None:
        return None
    normalized = str(value).strip().lower()
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()

# Sample raw profiles: two banking (AppsFlyer x2 + CoreBanking) rows that
# should resolve into ONE banking master profile (chained via device_id then
# phone_number), and three retail rows (MoEngage + WebTracking + a second,
# unrelated MoEngage customer) that should resolve into TWO retail master
# profiles (one merged via shared email, one standalone).
#
# NOTE: full_name/email/phone_number/national_id below are plaintext ONLY in
# this source file (for readability); seed_raw_profiles() SHA-256 hashes them
# via hash_pii() before anything is written to the database.
RAW_PROFILES = [
    {
        "domain": "banking",
        "source_system": "AppsFlyer",
        "channel": "mobile_app",
        "full_name": None,
        "email": None,
        "phone_number": None,
        "national_id": None,
        "device_id": "device-bank-0001",
        "advertising_id": "af-idfa-0001",
        "platform": "ios",
        "app_version": "3.4.2",
        "media_source": "Facebook Ads",
        "campaign": "vn_bank123_credit_card_acquisition_q4",
        "event_name": "install",
    },
    {
        "domain": "banking",
        "source_system": "AppsFlyer",
        "channel": "mobile_app",
        "full_name": None,
        "email": None,
        "phone_number": "0901234567",
        "national_id": None,
        "device_id": "device-bank-0001",  # same device as the install event above
        "advertising_id": "af-idfa-0001",
        "external_customer_id": "bank123_cust_9001",
        "platform": "ios",
        "app_version": "3.4.2",
        "event_name": "login",
    },
    {
        "domain": "banking",
        "source_system": "CoreBanking",
        "channel": "call_center",
        "full_name": "Nguyen Van A",
        "email": "nguyenvana@example.com",
        "phone_number": "0901234567",  # links to the AppsFlyer login row above
        "national_id": "079123456789",
        "event_name": "kyc_completed",
    },
    {
        "domain": "retail",
        "source_system": "MoEngage",
        "channel": "mobile_app",
        "full_name": "Tran Thi B",
        "email": "tranthib@example.com",
        "phone_number": None,
        "external_customer_id": "moe_cust_5002",
        "push_token": "fcm-push-token-5002",
        "platform": "android",
        "event_name": "app_open",
    },
    {
        "domain": "retail",
        "source_system": "WebTracking",
        "channel": "web",
        "full_name": None,
        "email": "tranthib@example.com",  # same email as the MoEngage row above
        "cookie_id": "web-cookie-5002",
        "ga_client_id": "GA1.2.111111.222222",
        "session_id": "sess-abc-123",
        "utm_source": "google",
        "utm_medium": "cpc",
        "utm_campaign": "retail_summer_sale",
        "event_name": "page_view",
    },
    {
        "domain": "retail",
        "source_system": "MoEngage",
        "channel": "mobile_app",
        "full_name": "Le Van C",
        "email": "levanc@example.com",  # unrelated customer -> its own master profile
        "external_customer_id": "moe_cust_5003",
        "push_token": "fcm-push-token-5003",
        "platform": "android",
        "event_name": "app_open",
    },
]

RAW_COLUMNS = (
    "tenant_id", "domain", "source_system", "channel", "external_customer_id",
    "full_name", "email", "phone_number", "national_id", "device_id",
    "advertising_id", "platform", "app_version", "push_token", "cookie_id",
    "ga_client_id", "session_id", "media_source", "campaign", "utm_source",
    "utm_medium", "utm_campaign", "event_name",
)


def _table(name: str) -> str:
    return f"{DB_SCHEMA}.{name}" if DB_SCHEMA else name


def ensure_extensions(cursor) -> None:
    logger.info("Ensuring pg_trgm / fuzzystrmatch extensions are enabled...")
    cursor.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm;")
    cursor.execute("CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;")


def ensure_cir_metadata_tables(cursor) -> None:
    """Creates the CIR runtime tables if they don't already exist.

    These are additive/idempotent (CREATE TABLE IF NOT EXISTS) and safe to
    run against a database already populated from
    core-customer360/database-schema.sql.
    """
    logger.info("Ensuring cdp_profile_attributes table exists...")
    cursor.execute(f"""
        CREATE TABLE IF NOT EXISTS {_table('cdp_profile_attributes')} (
            id BIGSERIAL PRIMARY KEY,
            attribute_internal_code VARCHAR(100) UNIQUE NOT NULL,
            name VARCHAR(255) NOT NULL,
            status VARCHAR(50) DEFAULT 'ACTIVE',
            data_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
            is_identity_resolution BOOLEAN DEFAULT FALSE,
            matching_rule VARCHAR(50) NULL,
            matching_threshold DECIMAL(5, 4) NULL,
            consolidation_rule VARCHAR(50) NULL,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );
    """)

    logger.info("Ensuring cdp_id_resolution_status table exists...")
    cursor.execute(f"""
        CREATE TABLE IF NOT EXISTS {_table('cdp_id_resolution_status')} (
            id BOOLEAN PRIMARY KEY DEFAULT TRUE,
            last_executed_at TIMESTAMP WITH TIME ZONE,
            CONSTRAINT enforce_one_row CHECK (id = TRUE)
        );
    """)
    cursor.execute(f"""
        INSERT INTO {_table('cdp_id_resolution_status')} (id, last_executed_at)
        VALUES (TRUE, NULL)
        ON CONFLICT (id) DO NOTHING;
    """)


def seed_matching_rules(cursor) -> None:
    logger.info("Seeding %d identity resolution matching rules...", len(MATCHING_RULES))
    for code, name, is_ir, rule, threshold, consolidation in MATCHING_RULES:
        cursor.execute(
            f"""
            INSERT INTO {_table('cdp_profile_attributes')}
                (attribute_internal_code, name, status, is_identity_resolution,
                 matching_rule, matching_threshold, consolidation_rule)
            VALUES (%s, %s, 'ACTIVE', %s, %s, %s, %s)
            ON CONFLICT (attribute_internal_code) DO UPDATE SET
                name = EXCLUDED.name,
                status = 'ACTIVE',
                is_identity_resolution = EXCLUDED.is_identity_resolution,
                matching_rule = EXCLUDED.matching_rule,
                matching_threshold = EXCLUDED.matching_threshold,
                consolidation_rule = EXCLUDED.consolidation_rule;
            """,
            (code, name, is_ir, rule, threshold, consolidation),
        )


def reset_demo_data(cursor) -> None:
    """Deletes any previous demo data, scoped strictly to DEMO_TENANT_ID."""
    logger.info("Resetting previous demo data for tenant_id=%s...", DEMO_TENANT_ID)
    cursor.execute(
        f"DELETE FROM {_table('cdp_profile_links')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )
    cursor.execute(
        f"DELETE FROM {_table('cdp_master_profiles')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )
    cursor.execute(
        f"DELETE FROM {_table('cdp_raw_profiles_stage')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )


def seed_raw_profiles(cursor) -> None:
    """Inserts the sample raw profiles, SHA-256 hashing every PII field
    (see HASHED_PII_FIELDS) so no plaintext name/email/phone/national_id is
    ever written to the database."""
    logger.info("Inserting %d sample raw profiles (AppsFlyer/MoEngage/WebTracking)...", len(RAW_PROFILES))
    columns = ", ".join(RAW_COLUMNS)
    placeholders = ", ".join(["%s"] * len(RAW_COLUMNS))
    insert_query = f"""
        INSERT INTO {_table('cdp_raw_profiles_stage')} ({columns})
        VALUES ({placeholders});
    """
    for profile in RAW_PROFILES:
        values = []
        for col in RAW_COLUMNS[1:]:
            value = profile.get(col)
            if col in HASHED_PII_FIELDS:
                value = hash_pii(value)
            values.append(value)
        row = [DEMO_TENANT_ID] + values
        cursor.execute(insert_query, row)


def main() -> None:
    conn = psycopg2.connect(
        host=DB_HOST, dbname=DB_NAME, user=DB_USER, password=DB_PASSWORD, port=DB_PORT
    )
    try:
        with conn.cursor() as cursor:
            ensure_extensions(cursor)
            ensure_cir_metadata_tables(cursor)
            seed_matching_rules(cursor)
            reset_demo_data(cursor)
            seed_raw_profiles(cursor)
        conn.commit()
        logger.info(
            "Sample data ready: %d raw profiles staged with status_code=1 for tenant_id=%s.",
            len(RAW_PROFILES),
            DEMO_TENANT_ID,
        )
    except Exception:
        conn.rollback()
        logger.exception("Failed to seed sample data.")
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
