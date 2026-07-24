"""Seeds sample test data for a Customer Identity Resolution (CIR) demo run.

Connects to the PostgreSQL database configured via environment variables /
``.env`` (see ``daily_job.py``) and:

1. Ensures the ``pg_trgm`` / ``fuzzystrmatch`` extensions are enabled (needed
   for the ``fuzzy_dmetaphone`` matching rule used by other attributes).
2. Ensures the ``cdp_profile_attributes`` (full attribute catalog + CIR
   matching-rule metadata, now part of ``core-customer360/database-schema.sql``)
   and ``cdp_id_resolution_status`` (real-time throttle state, still a CIR
   runtime-only table) exist. The ``CREATE TABLE IF NOT EXISTS`` here is only
   a defensive fallback for databases where ``database-schema.sql`` has not
   been (re)applied yet; it is a no-op once that schema has been migrated in.
3. Seeds/upserts the active identity-resolution matching rules (only the
   matching-rule-specific columns -- the full attribute metadata is owned by
   ``database-schema.sql``'s seed data).
4. Clears any previous demo data (scoped to ``DEMO_TENANT_ID`` only, so it
   never touches other tenants) and inserts 1,000 generated raw profiles
   simulating AppsFlyer mobile attribution across multiple acquisition
   channels (Facebook Ads, TikTok Ads, Google Ads, Grab Ads, FPT Play Ads,
   and offline PR events at shopping malls), for both the banking and
   retail domains. ~30% of the rows are deliberate duplicates (a later
   touch on a device that already appears earlier in the batch) for
   identity resolution to merge back together -- see generate_raw_profiles().

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
import random
from datetime import datetime, timedelta

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


# --- Synthetic AppsFlyer data generation ------------------------------------
#
# AppsFlyer channel/media_source configuration: Facebook Ads, TikTok Ads,
# Google Ads, Grab Ads, FPT Play Ads (video/OTT), and offline PR events
# (e.g. roadshows / registration booths at shopping malls) that still get
# attributed back into the app via AppsFlyer OneLink QR codes.
APPSFLYER_CHANNELS = [
    {"media_source": "Facebook Ads", "campaigns": ["vn_retail_fb_flash_sale", "vn_bank123_creditcard_fb_q4"]},
    {"media_source": "TikTok Ads", "campaigns": ["vn_tiktok_genz_promo", "vn_tiktok_livestream_sale"]},
    {"media_source": "Google Ads", "campaigns": ["vn_google_search_brand", "vn_google_display_retargeting"]},
    {"media_source": "Grab Ads", "campaigns": ["vn_grab_inapp_banner", "vn_grab_loyalty_crosspromo"]},
    {"media_source": "FPT Play Ads", "campaigns": ["vn_fptplay_video_preroll", "vn_fptplay_channel_sponsorship"]},
    {"media_source": "Offline PR Event", "campaigns": ["hcmc_shopping_mall_roadshow", "hanoi_mall_activation_day"]},
]

VIETNAMESE_FAMILY_NAMES = (
    "Nguyen", "Tran", "Le", "Pham", "Hoang", "Huynh", "Phan", "Vu", "Vo", "Dang", "Bui", "Do", "Ho", "Ngo", "Duong",
)
VIETNAMESE_MIDDLE_NAMES = ("Van", "Thi", "Huu", "Minh", "Ngoc", "Thanh", "Quang", "Xuan")
VIETNAMESE_GIVEN_NAMES = (
    "An", "Binh", "Chi", "Dung", "Giang", "Ha", "Hoa", "Huong", "Khanh", "Lan", "Linh",
    "Long", "Mai", "Nam", "Nga", "Phuong", "Quan", "Son", "Thao", "Thu", "Trang", "Tuan", "Yen",
)

RETAIL_TOUCH_EVENTS = ("login", "app_open", "purchase")
BANKING_TOUCH_EVENTS = ("login", "kyc_completed", "loan_application")

# Share of synthetic customers whose app belongs to the banking domain
# (the rest are retail). Tune here if you want a different domain mix.
BANKING_DOMAIN_SHARE = 0.4


def _random_full_name(rng: random.Random) -> str:
    return " ".join(
        [rng.choice(VIETNAMESE_FAMILY_NAMES), rng.choice(VIETNAMESE_MIDDLE_NAMES), rng.choice(VIETNAMESE_GIVEN_NAMES)]
    )


def _build_customer(rng: random.Random, index: int, used_names: set, used_phones: set) -> dict:
    """Creates one synthetic person's stable identity (device + PII), shared
    across every raw-profile row generated for that person.

    full_name and phone_number are regenerated on collision so two different
    synthetic customers never accidentally share one -- both are active
    matching rules, so a coincidental collision would make identity
    resolution incorrectly merge two distinct people into one profile.
    """
    domain = "banking" if rng.random() < BANKING_DOMAIN_SHARE else "retail"
    platform = "ios" if rng.random() < 0.5 else "android"
    channel = rng.choice(APPSFLYER_CHANNELS)

    full_name = _random_full_name(rng)
    while full_name in used_names:
        full_name = _random_full_name(rng)
    used_names.add(full_name)

    phone_number = f"09{rng.randint(10000000, 99999999)}"
    while phone_number in used_phones:
        phone_number = f"09{rng.randint(10000000, 99999999)}"
    used_phones.add(phone_number)

    email_slug = full_name.lower().replace(" ", ".")

    return {
        "domain": domain,
        "platform": platform,
        "media_source": channel["media_source"],
        "campaign": rng.choice(channel["campaigns"]),
        "device_id": f"device-{index:05d}-{rng.randint(1000, 9999)}",
        "advertising_id": f"af-{'idfa' if platform == 'ios' else 'gaid'}-{index:05d}",
        "external_customer_id": f"appsflyer_cust_{index:05d}",
        "full_name": full_name,
        "email": f"{email_slug}{index}@example.com",
        "phone_number": phone_number,
        "national_id": (
            f"{rng.randint(10, 99)}{rng.randint(1000000000, 9999999999)}" if domain == "banking" else None
        ),
    }


def _install_event(customer: dict, event_time: datetime) -> dict:
    """First AppsFlyer touch: an anonymous install -- no PII revealed yet,
    only the device/advertising id and acquisition channel."""
    return {
        "domain": customer["domain"],
        "source_system": "AppsFlyer",
        "channel": "mobile_app",
        "device_id": customer["device_id"],
        "advertising_id": customer["advertising_id"],
        "platform": customer["platform"],
        "app_version": "3.4.2",
        "media_source": customer["media_source"],
        "campaign": customer["campaign"],
        "event_name": "install",
        "event_time": event_time,
    }


def _touch_event(rng: random.Random, customer: dict, event_time: datetime) -> dict:
    """A later AppsFlyer touch (login/purchase/kyc/...) on the SAME device
    that reveals the customer's PII. This is the duplicate row identity
    resolution is expected to merge back into the install event's profile
    (matched via the shared device_id/advertising_id)."""
    events = BANKING_TOUCH_EVENTS if customer["domain"] == "banking" else RETAIL_TOUCH_EVENTS
    return {
        "domain": customer["domain"],
        "source_system": "AppsFlyer",
        "channel": "mobile_app",
        "external_customer_id": customer["external_customer_id"],
        "full_name": customer["full_name"],
        "email": customer["email"],
        "phone_number": customer["phone_number"],
        "national_id": customer["national_id"],
        "device_id": customer["device_id"],
        "advertising_id": customer["advertising_id"],
        "platform": customer["platform"],
        "app_version": "3.4.2",
        "media_source": customer["media_source"],
        "campaign": customer["campaign"],
        "event_name": rng.choice(events),
        "event_time": event_time,
    }


def generate_raw_profiles(count: int = 1000, duplicate_rate: float = 0.3, seed: int = 42) -> list[dict]:
    """Generates ``count`` synthetic AppsFlyer raw-profile events spread
    across Facebook Ads, TikTok Ads, Google Ads, Grab Ads, FPT Play Ads and
    offline PR events at shopping malls, for both the retail and banking
    domains.

    ~``duplicate_rate`` of the rows are a second (or third, ...) touch on a
    device that already appears earlier in the batch (same device_id /
    advertising_id, revealing full_name/email/phone_number/national_id) --
    real duplicate profiles that identity resolution is expected to merge
    back into a single master profile.

    Uses a fixed ``seed`` so re-running this script produces the exact same
    dataset (consistent with the rest of this script being idempotent).
    """
    rng = random.Random(seed)
    num_unique = max(1, round(count * (1 - duplicate_rate)))
    num_duplicates = count - num_unique

    base_time = datetime.now() - timedelta(days=60)
    used_names: set = set()
    used_phones: set = set()
    customers = [_build_customer(rng, i, used_names, used_phones) for i in range(num_unique)]
    profiles = [_install_event(customer, base_time + timedelta(minutes=i)) for i, customer in enumerate(customers)]

    for _ in range(num_duplicates):
        customer = rng.choice(customers)
        touch_time = base_time + timedelta(days=rng.randint(1, 45), hours=rng.randint(0, 23), minutes=rng.randint(0, 59))
        profiles.append(_touch_event(rng, customer, touch_time))

    rng.shuffle(profiles)
    logger.info(
        "Generated %d raw profiles (%d unique customers, %d duplicate touches, ~%.0f%% duplicate rate).",
        len(profiles), num_unique, num_duplicates, duplicate_rate * 100,
    )
    return profiles


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

    ``cdp_profile_attributes`` is now defined canonically in
    core-customer360/database-schema.sql (full attribute catalog: identity /
    demographic / retail / banking / marketing / lineage columns plus Lead /
    Churn / CLV / CX / Data Quality scoring-model metadata). The
    ``CREATE TABLE IF NOT EXISTS`` below only matters as a fallback for a
    database where that schema file has not been applied yet -- it mirrors
    the same full column set so this script keeps working standalone; it is
    a no-op once database-schema.sql has created the table.
    """
    logger.info("Ensuring cdp_profile_attributes table exists...")
    cursor.execute(f"""
        CREATE TABLE IF NOT EXISTS {_table('cdp_profile_attributes')} (
            id BIGSERIAL PRIMARY KEY,
            attribute_internal_code VARCHAR(100) UNIQUE NOT NULL,
            master_profile_column VARCHAR(100),
            name VARCHAR(255) NOT NULL,
            description TEXT,
            attribute_group VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
            source_table VARCHAR(150) NOT NULL DEFAULT 'cdp_master_profiles',
            data_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
            domain_scope VARCHAR(20) NOT NULL DEFAULT 'all',
            is_pii BOOLEAN NOT NULL DEFAULT FALSE,
            status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
            is_identity_resolution BOOLEAN NOT NULL DEFAULT FALSE,
            matching_rule VARCHAR(50) NULL,
            matching_threshold DECIMAL(5, 4) NULL,
            consolidation_rule VARCHAR(50) NULL,
            is_scoring_model BOOLEAN NOT NULL DEFAULT FALSE,
            scoring_model_name VARCHAR(100),
            scoring_model_version VARCHAR(20),
            value_type VARCHAR(50),
            value_min NUMERIC,
            value_max NUMERIC,
            refresh_frequency VARCHAR(50),
            display_order INT NOT NULL DEFAULT 0,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
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
    """Deletes any previous demo data, scoped strictly to DEMO_TENANT_ID.

    Also clears every OTHER table that FK-references cdp_master_profiles /
    cdp_raw_profiles_stage and gets populated by
    scripts/seed_full_demo_data.py (cdp_raw_events, crm_transactions,
    crm_customer_contacts, cdp_relations) -- those must be deleted BEFORE
    cdp_master_profiles/cdp_raw_profiles_stage or re-running this script
    after seed_full_demo_data.py has run raises a ForeignKeyViolation (the
    old master profiles are still referenced from those tables).
    """
    logger.info("Resetting previous demo data for tenant_id=%s...", DEMO_TENANT_ID)
    cursor.execute(
        f"DELETE FROM {_table('cdp_raw_events')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )
    cursor.execute(
        f"DELETE FROM {_table('crm_transactions')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )
    cursor.execute(
        f"DELETE FROM {_table('crm_customer_contacts')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )
    cursor.execute(
        f"DELETE FROM {_table('cdp_relations')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )
    cursor.execute(
        f"DELETE FROM {_table('cdp_profile_links')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )
    cursor.execute(
        f"DELETE FROM {_table('cdp_master_profiles')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )
    cursor.execute(
        f"DELETE FROM {_table('cdp_raw_profiles_stage')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,)
    )


def seed_raw_profiles(cursor, raw_profiles: list[dict]) -> None:
    """Inserts the sample raw profiles, SHA-256 hashing every PII field
    (see HASHED_PII_FIELDS) so no plaintext name/email/phone/national_id is
    ever written to the database."""
    logger.info("Inserting %d sample raw profiles (AppsFlyer)...", len(raw_profiles))
    columns = ", ".join(RAW_COLUMNS)
    placeholders = ", ".join(["%s"] * len(RAW_COLUMNS))
    insert_query = f"""
        INSERT INTO {_table('cdp_raw_profiles_stage')} ({columns})
        VALUES ({placeholders});
    """
    for profile in raw_profiles:
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
        raw_profiles = generate_raw_profiles(count=1000, duplicate_rate=0.3)
        with conn.cursor() as cursor:
            ensure_extensions(cursor)
            ensure_cir_metadata_tables(cursor)
            seed_matching_rules(cursor)
            reset_demo_data(cursor)
            seed_raw_profiles(cursor, raw_profiles)
        conn.commit()
        logger.info(
            "Sample data ready: %d raw profiles staged with status_code=1 for tenant_id=%s.",
            len(raw_profiles),
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
