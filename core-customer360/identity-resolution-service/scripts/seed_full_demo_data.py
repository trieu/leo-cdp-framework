"""Seeds comprehensive demo data covering every table/column in
core-customer360/database-schema.sql that ``init_sample_data.py`` +
``run_demo_resolution.py`` do NOT already exercise.

Those two scripts only cover the Customer Identity Resolution (CIR) slice:
AppsFlyer raw-profile ingestion -> resolved ``cdp_master_profiles`` rows. This
script MUST run AFTER ``run_demo_resolution.py`` (see ``run-demo.sh``) so it
can enrich the already-resolved master profiles and link new demo rows to
real ``master_profile_id`` values. It covers:

1. CRM Journey Graph: ``crm_industry``, ``crm_account``, ``crm_lead_source``,
   ``crm_lead``, ``crm_campaign``, ``crm_campaign_member``, ``crm_contact``,
   ``crm_opportunity`` -- the Lead -> CampaignMember -> Contact -> Opportunity
   B2B journey described in CIR-Tech-Slides-VN.md / README.md.
2. Relations: ``cdp_relation_types`` (friend/colleague/family/customer-contact)
   + ``cdp_relations`` linking real resolved master profiles together.
3. ``crm_customer_contacts`` (CS/call-center/email interaction log) and
   ``crm_transactions`` (retail purchases, banking transfers/payments --
   including a couple of NOT-YET-identity-resolved rows with
   ``master_profile_id = NULL``, the same async-backfill pattern used by
   ``cdp_raw_events``).
4. ``cdp_raw_events``: sample behavioral events spanning every
   ``event_category`` seeded in ``cdp_event_catalog`` (GENERAL/FEEDBACK/
   COMMERCE/FINANCE/STOCK_TRADING/TRAVEL/REAL_ESTATE), including a few
   travel/real_estate events with NO master profile yet (domains not
   otherwise represented among the AppsFlyer-only CIR demo profiles).
5. ``graph_edges``: a handful of edges spanning several relation partitions
   (``belongs_to``, ``converted``, ``has``, ``belongs_to_industry``,
   ``is_connected_to``, ``is_from``).
6. ``cdp_master_profiles`` enrichment: fills in every column NOT already set
   by ``CustomerIdentityResolver`` -- lifecycle/engagement tracking
   (customer_since/last_activity_at/preferred_channel/lifecycle_stage/
   persona_summary), the full ML scoring block (lead/churn/CLV/CX/data
   quality), retail-only attrs (loyalty_id/membership_tier/
   preferred_store_code) for retail-domain profiles, banking-only attrs
   (cif_number/account_numbers/kyc_status/risk_segment) for banking-domain
   profiles, acquisition_source/acquisition_campaign (joined back from the
   raw profile that first created the master, via first_seen_raw_profile_id),
   segmentation_tags/attributes/gender/address/profile_picture_url, and a
   ``persona_embedding`` vector for a representative subset of profiles.
7. **crm_contact <-> cdp_master_profiles linkage**: these two tables have NO
   shared key in database-schema.sql (crm_contact has no tenant_id/
   master_profile_id column, and cdp_master_profiles has nothing pointing
   back to crm_contact) -- they represent separate B2B-CRM vs B2C-identity-
   resolution domains. ``link_crm_contacts_to_master_profiles()`` bridges a
   handful of them via the generic ``graph_edges`` table
   (``relation = 'is_active_as'``, ``cdp_master_profiles -> crm_contact``),
   PLUS a denormalized cross-reference id on each side
   (``cdp_master_profiles.attributes->>'linked_crm_contact_id'`` and
   ``crm_contact.metadata->>'linked_master_profile_id'``) so the link is
   discoverable/joinable from either table without necessarily touching
   graph_edges.

Deliberately NOT populated (left NULL / default), consistent with this
demo's existing "never store plaintext PII" policy for identity-resolution
tables (see init_sample_data.py's hash_pii()): ``first_name``/``last_name``
(no plaintext name is available -- full_name is a one-way hash),
``secondary_emails``/``secondary_phones``, and ``date_of_birth``. ``address``
is populated with city/country only (no street). ``gender`` and
``profile_picture_url`` ARE populated -- neither is independently
identifying PII.

Note: ``crm_lead``/``crm_contact`` DO get plaintext first/last name/email/
phone -- that's a *different* table representing a separate use case (a
Salesforce-style B2B CRM record), not the hashed-PII identity-resolution
pipeline, and the schema itself defines those columns as plain TEXT with no
hashing expectation. Names used are obviously-synthetic demo placeholders.

Idempotent / safe to re-run:
- CRM entities (crm_industry/crm_account/.../crm_opportunity) have NO
  tenant_id column in database-schema.sql, so they're keyed by deterministic
  uuid5 ids (derived from a fixed demo string) and upserted via
  ``ON CONFLICT (pk) DO UPDATE``.
- ``cdp_relation_types`` is upserted via ``ON CONFLICT (code) DO NOTHING``.
- Every tenant-scoped table seeded here (cdp_relations, crm_customer_contacts,
  crm_transactions, cdp_raw_events) is reset (``DELETE ... WHERE tenant_id =
  DEMO_TENANT_ID``) before reinserting.
- ``graph_edges`` has no tenant_id either -- demo rows are tagged
  ``metadata->>'demo_tenant' = DEMO_TENANT_ID`` and reset via that filter.
- ``cdp_master_profiles`` enrichment is a plain UPDATE keyed by
  ``master_profile_id`` -- naturally idempotent (re-running just recomputes
  the same deterministic values, since every generator below is seeded from
  the profile's own id).
- ``link_crm_contacts_to_master_profiles()``'s ``graph_edges`` rows are covered
  by the same ``reset_tenant_scoped_demo_tables()`` delete (tagged
  ``metadata->>'demo_tenant'``) as ``seed_graph_edges()``, so re-running never
  duplicates them; the ``attributes``/``metadata`` cross-reference UPDATEs are
  independently idempotent too (same key -> same value every time via jsonb
  ``||`` merge).
"""

import hashlib
import logging
import os
import random
import uuid
from datetime import datetime, timedelta

import psycopg2
from dotenv import load_dotenv
from psycopg2.extras import Json, RealDictCursor

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

DB_HOST = os.environ.get("DB_HOST", "localhost")
DB_NAME = os.environ.get("DB_NAME", "customer360")
DB_USER = os.environ.get("DB_USER", "postgres")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "password")
DB_PORT = os.environ.get("DB_PORT", "5432")
DB_SCHEMA = os.environ.get("DB_SCHEMA", "customer360")

# Must match scripts/init_sample_data.py / scripts/run_demo_resolution.py.
DEMO_TENANT_ID = "11111111-1111-1111-1111-111111111111"

# Fixed namespace so every "demo:<key>" -> deterministic UUID, making the
# tenant-less CRM entity tables safe to re-seed without ever duplicating rows.
DEMO_NAMESPACE = uuid.UUID("12345678-1234-5678-1234-567812345678")

# How many resolved master profiles get the heavier per-row demo content
# (customer contacts / transactions / raw events / persona_embedding). All
# master profiles still get the lightweight lifecycle+scoring enrichment.
DETAIL_PROFILE_LIMIT = 60
EMBEDDING_PROFILE_LIMIT = 30
PERSONA_EMBEDDING_DIM = 768


def _table(name: str) -> str:
    return f"{DB_SCHEMA}.{name}" if DB_SCHEMA else name


def demo_id(key: str) -> str:
    """Deterministic UUID for a given demo entity key -- makes tenant-less
    CRM tables safe to re-seed (same key always -> same primary key)."""
    return str(uuid.uuid5(DEMO_NAMESPACE, key))


def stable_rng(key: str) -> random.Random:
    """A random.Random seeded deterministically from ``key`` (e.g. a
    master_profile_id) -- keeps this whole script idempotent."""
    seed = int(hashlib.sha256(key.encode("utf-8")).hexdigest(), 16) % (2**32)
    return random.Random(seed)


# --------------------------------------------------------------------------
# 1. CRM Journey Graph
# --------------------------------------------------------------------------

INDUSTRIES = [
    ("Banking & Financial Services", "Retail and commercial banking, wealth management."),
    ("Retail & E-commerce", "Omni-channel retail, marketplaces and D2C brands."),
    ("Real Estate", "Residential and commercial property developers/agencies."),
    ("Travel & Hospitality", "Airlines, OTAs, hotel groups and tour operators."),
]

ACCOUNTS = [
    ("Sacombank Digital", "Banking & Financial Services"),
    ("Techcombank Wealth Partners", "Banking & Financial Services"),
    ("VinMart Retail Group", "Retail & E-commerce"),
    ("Saigon Co.op Omnichannel", "Retail & E-commerce"),
    ("Danh Khoi Real Estate", "Real Estate"),
    ("Vietravel Holdings", "Travel & Hospitality"),
]

LEAD_SOURCES = [
    ("Website Contact Form", "Inbound leads from the corporate website."),
    ("Trade Show", "Leads captured at industry conferences/booths."),
    ("Referral Partner", "Leads referred by an existing customer or partner."),
    ("Cold Outreach", "Outbound SDR prospecting (email/call)."),
    ("Paid Search", "Leads from Google/Bing search ads."),
]

CAMPAIGNS = [
    ("Q4 Enterprise Banking Outreach", "banking", -60, 30),
    ("Retail Partner Roadshow", "retail", -45, 15),
    ("Real Estate Investor Webinar Series", "real_estate", -30, 60),
]

LEAD_FIRST_NAMES = ("Minh", "Linh", "Huy", "Trang", "Khoa", "My", "Duc", "Anh")
LEAD_LAST_NAMES = ("Nguyen", "Tran", "Le", "Pham", "Hoang", "Vo", "Bui", "Dang")


def seed_relation_types(cursor) -> None:
    logger.info("Seeding cdp_relation_types...")
    for code, description in (
        ("friend", "Personal friendship between two profiles."),
        ("colleague", "Coworker relationship between two profiles."),
        ("family", "Family/household relationship between two profiles."),
        ("customer-contact", "One profile referred or is a point of contact for another."),
    ):
        cursor.execute(
            f"""
            INSERT INTO {_table('cdp_relation_types')} (code, description)
            VALUES (%s, %s)
            ON CONFLICT (code) DO UPDATE SET description = EXCLUDED.description;
            """,
            (code, description),
        )


def seed_crm_entities(cursor) -> dict:
    """Seeds the CRM journey graph and returns a dict of the demo entity ids
    keyed by kind (for cross-referencing from graph_edges)."""
    logger.info("Seeding CRM journey graph (industries/accounts/lead sources/leads/campaigns/...)...")
    ids: dict = {"industry": {}, "account": {}, "lead_source": {}, "lead": [], "campaign": {}, "contact": [], "opportunity": []}

    for name, description in INDUSTRIES:
        industry_id = demo_id(f"crm_industry:{name}")
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_industry')} (industry_id, name, description, keywords)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (industry_id) DO UPDATE SET description = EXCLUDED.description;
            """,
            (industry_id, name, description, [name.lower().replace(" & ", "_").replace(" ", "_")]),
        )
        ids["industry"][name] = industry_id

    for name, industry_name in ACCOUNTS:
        account_id = demo_id(f"crm_account:{name}")
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_account')} (account_id, name, industry_id, description, keywords)
            VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (account_id) DO UPDATE SET industry_id = EXCLUDED.industry_id;
            """,
            (account_id, name, ids["industry"][industry_name], f"Demo account in {industry_name}.", [industry_name]),
        )
        ids["account"][name] = account_id

    for name, description in LEAD_SOURCES:
        lead_source_id = demo_id(f"crm_lead_source:{name}")
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_lead_source')} (lead_source_id, name, description)
            VALUES (%s, %s, %s)
            ON CONFLICT (lead_source_id) DO UPDATE SET description = EXCLUDED.description;
            """,
            (lead_source_id, name, description),
        )
        ids["lead_source"][name] = lead_source_id

    for name, domain, start_offset, end_offset in CAMPAIGNS:
        campaign_id = demo_id(f"crm_campaign:{name}")
        today = datetime.now().date()
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_campaign')}
                (campaign_id, name, description, keywords, start_date, end_date, metadata)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (campaign_id) DO UPDATE SET
                start_date = EXCLUDED.start_date, end_date = EXCLUDED.end_date;
            """,
            (
                campaign_id, name, f"Demo marketing campaign targeting the {domain} domain.",
                [domain], today + timedelta(days=start_offset), today + timedelta(days=end_offset),
                Json({"domain": domain}),
            ),
        )
        ids["campaign"][name] = campaign_id

    rng = stable_rng("crm_leads")
    lead_source_names = list(ids["lead_source"].keys())
    for i in range(8):
        lead_id = demo_id(f"crm_lead:{i}")
        first_name = rng.choice(LEAD_FIRST_NAMES)
        last_name = rng.choice(LEAD_LAST_NAMES)
        source_name = rng.choice(lead_source_names)
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_lead')}
                (lead_id, first_name, last_name, email, phone, description, keywords, metadata)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (lead_id) DO UPDATE SET description = EXCLUDED.description;
            """,
            (
                lead_id, first_name, last_name,
                f"demo.lead{i}@example.com", f"09{rng.randint(10000000, 99999999)}",
                f"Synthetic demo lead sourced via {source_name}.", [source_name],
                Json({"lead_source": source_name, "synthetic": True}),
            ),
        )
        ids["lead"].append(lead_id)

    contact_defs = [
        ("Sacombank Digital", 0), ("Techcombank Wealth Partners", 1),
        ("VinMart Retail Group", 2), ("Saigon Co.op Omnichannel", 3),
        ("Danh Khoi Real Estate", 4), ("Vietravel Holdings", 5),
    ]
    for account_name, lead_index in contact_defs:
        contact_id = demo_id(f"crm_contact:{account_name}")
        first_name = rng.choice(LEAD_FIRST_NAMES)
        last_name = rng.choice(LEAD_LAST_NAMES)
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_contact')}
                (contact_id, first_name, last_name, email, phone, account_id, description, metadata)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (contact_id) DO UPDATE SET account_id = EXCLUDED.account_id;
            """,
            (
                contact_id, first_name, last_name,
                f"{first_name.lower()}.{last_name.lower()}@{account_name.lower().split()[0]}.example.com",
                f"09{rng.randint(10000000, 99999999)}", ids["account"][account_name],
                f"Primary contact at {account_name}, converted from a demo lead.",
                Json({"converted_from_lead_id": ids["lead"][lead_index]}),
            ),
        )
        ids["contact"].append(contact_id)

    opportunity_defs = [
        ("Sacombank Digital", "Digital Banking Platform Renewal", 1_200_000_000, "negotiation", 45),
        ("VinMart Retail Group", "Loyalty Program Expansion", 350_000_000, "proposal", 30),
        ("Danh Khoi Real Estate", "CRM + Customer 360 Rollout", 800_000_000, "qualification", 90),
        ("Vietravel Holdings", "Booking Personalization Engine", 600_000_000, "closed_won", -10),
    ]
    for account_name, opp_name, value, stage, close_offset in opportunity_defs:
        opportunity_id = demo_id(f"crm_opportunity:{opp_name}")
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_opportunity')}
                (opportunity_id, account_id, name, value, stage, close_date, description)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (opportunity_id) DO UPDATE SET stage = EXCLUDED.stage, value = EXCLUDED.value;
            """,
            (
                opportunity_id, ids["account"][account_name], opp_name, value, stage,
                datetime.now().date() + timedelta(days=close_offset),
                f"Demo opportunity with {account_name}.",
            ),
        )
        ids["opportunity"].append(opportunity_id)

    # Campaign members: some already-converted contacts responding to a
    # campaign, plus some still-unconverted leads (contact_id left NULL,
    # original lead tracked in metadata).
    campaign_names = list(ids["campaign"].keys())
    for i, contact_id in enumerate(ids["contact"]):
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_campaign_member')}
                (campaign_member_id, campaign_id, contact_id, status, description)
            VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (campaign_member_id) DO UPDATE SET status = EXCLUDED.status;
            """,
            (
                demo_id(f"crm_campaign_member:contact:{contact_id}"),
                ids["campaign"][campaign_names[i % len(campaign_names)]],
                contact_id, "converted", "Already-converted contact who engaged with this campaign.",
            ),
        )
    for i, lead_id in enumerate(ids["lead"][:4]):
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_campaign_member')}
                (campaign_member_id, campaign_id, contact_id, status, description, metadata)
            VALUES (%s, %s, NULL, %s, %s, %s)
            ON CONFLICT (campaign_member_id) DO UPDATE SET status = EXCLUDED.status;
            """,
            (
                demo_id(f"crm_campaign_member:lead:{lead_id}"),
                ids["campaign"][campaign_names[i % len(campaign_names)]],
                "responded", "Lead responded to campaign but has not converted to a Contact yet.",
                Json({"lead_id": lead_id}),
            ),
        )

    return ids


# --------------------------------------------------------------------------
# 2-4. Relations, interactions, transactions, behavioral events
# --------------------------------------------------------------------------

def reset_tenant_scoped_demo_tables(cursor) -> None:
    logger.info("Resetting previous demo rows in tenant-scoped tables (relations/contacts/transactions/events)...")
    cursor.execute(f"DELETE FROM {_table('cdp_relations')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,))
    cursor.execute(f"DELETE FROM {_table('crm_customer_contacts')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,))
    cursor.execute(f"DELETE FROM {_table('crm_transactions')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,))
    cursor.execute(f"DELETE FROM {_table('cdp_raw_events')} WHERE tenant_id = %s;", (DEMO_TENANT_ID,))
    cursor.execute(f"DELETE FROM {_table('graph_edges')} WHERE metadata->>'demo_tenant' = %s;", (DEMO_TENANT_ID,))


def seed_relations(cursor, master_profiles: list) -> None:
    if len(master_profiles) < 4:
        logger.warning("Not enough master profiles to seed cdp_relations demo rows -- skipping.")
        return
    logger.info("Seeding cdp_relations (family/friend/customer-contact) between resolved master profiles...")
    banking = [m for m in master_profiles if m["domain"] == "banking"]
    retail = [m for m in master_profiles if m["domain"] == "retail"]

    def _link(a, b, code):
        cursor.execute(
            f"""
            INSERT INTO {_table('cdp_relations')}
                (tenant_id, source_master_id, target_master_id, relation_type_id)
            SELECT %s, %s, %s, relation_type_id FROM {_table('cdp_relation_types')} WHERE code = %s
            ON CONFLICT (tenant_id, source_master_id, target_master_id, relation_type_id) DO NOTHING;
            """,
            (DEMO_TENANT_ID, a, b, code),
        )

    if len(banking) >= 2:
        _link(banking[0]["master_profile_id"], banking[1]["master_profile_id"], "family")
    if len(retail) >= 2:
        _link(retail[0]["master_profile_id"], retail[1]["master_profile_id"], "friend")
    if banking and retail:
        _link(banking[0]["master_profile_id"], retail[0]["master_profile_id"], "customer-contact")


def seed_customer_contacts(cursor, master_profiles: list) -> None:
    logger.info("Seeding crm_customer_contacts (CS/call-center interaction log)...")
    channels = ("call_center", "live_chat", "email", "branch_visit")
    types = ("inquiry", "complaint", "feedback", "support_request")
    for m in master_profiles:
        rng = stable_rng(f"contacts:{m['master_profile_id']}")
        for _ in range(rng.randint(1, 3)):
            cursor.execute(
                f"""
                INSERT INTO {_table('crm_customer_contacts')}
                    (contact_id, tenant_id, master_profile_id, contact_type, contact_channel,
                     contact_content, contact_date)
                VALUES (%s, %s, %s, %s, %s, %s, %s);
                """,
                (
                    str(uuid.uuid4()), DEMO_TENANT_ID, m["master_profile_id"],
                    rng.choice(types), rng.choice(channels),
                    "Synthetic demo interaction log entry.",
                    datetime.now() - timedelta(days=rng.randint(1, 60), hours=rng.randint(0, 23)),
                ),
            )


RETAIL_TRANSACTIONS = [
    ("POS", "purchase", "product", "Retail Store Purchase", (100_000, 2_000_000), "pos"),
    ("WebStore", "purchase", "product", "Online Order", (150_000, 3_000_000), "web"),
]
BANKING_TRANSACTIONS = [
    ("CoreBanking", "transfer", "account", "Interbank Transfer", (500_000, 20_000_000), "mobile_app"),
    ("CoreBanking", "bill_payment", "account", "Utility Bill Payment", (100_000, 1_500_000), "internet_banking"),
]


def seed_transactions(cursor, master_profiles: list) -> None:
    logger.info("Seeding crm_transactions (retail purchases + banking transfers/payments)...")
    for m in master_profiles:
        rng = stable_rng(f"transactions:{m['master_profile_id']}")
        catalog = RETAIL_TRANSACTIONS if m["domain"] == "retail" else BANKING_TRANSACTIONS
        for _ in range(rng.randint(1, 3)):
            source_system, txn_type, entity_type, entity_name, amount_range, channel = rng.choice(catalog)
            cursor.execute(
                f"""
                INSERT INTO {_table('crm_transactions')}
                    (transaction_id, tenant_id, master_profile_id, source_system, transaction_type,
                     transaction_status, entity_type, entity_name, amount, currency, channel,
                     transaction_time)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
                """,
                (
                    str(uuid.uuid4()), DEMO_TENANT_ID, m["master_profile_id"], source_system, txn_type,
                    "completed", entity_type, entity_name, rng.randint(*amount_range), "VND", channel,
                    datetime.now() - timedelta(days=rng.randint(1, 90), hours=rng.randint(0, 23)),
                ),
            )

    # A couple of NOT-YET-resolved transactions (master_profile_id = NULL) --
    # demonstrates the same async-backfill pattern as cdp_raw_events, and the
    # ux_crm_transactions_tenant_source dedup-safety unique index.
    rng = stable_rng("unresolved_transactions")
    for i in range(2):
        cursor.execute(
            f"""
            INSERT INTO {_table('crm_transactions')}
                (transaction_id, tenant_id, master_profile_id, source_system, source_transaction_id,
                 transaction_type, transaction_status, entity_type, amount, currency, channel,
                 transaction_time)
            VALUES (%s, %s, NULL, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (tenant_id, source_system, source_transaction_id) WHERE source_transaction_id IS NOT NULL
            DO NOTHING;
            """,
            (
                str(uuid.uuid4()), DEMO_TENANT_ID, "POS", f"pos-unresolved-{i}",
                "purchase", "completed", "product", rng.randint(50_000, 500_000), "VND", "pos",
                datetime.now() - timedelta(hours=rng.randint(1, 48)),
            ),
        )


RETAIL_EVENTS = [
    ("GENERAL", "page-view", None, None, False),
    ("GENERAL", "search", None, None, False),
    ("COMMERCE", "add-to-cart", (100_000, 500_000), "product", False),
    ("COMMERCE", "purchase", (200_000, 2_000_000), "product", True),
    ("FEEDBACK", "submit-csat-form", None, None, False),
]
BANKING_EVENTS = [
    ("GENERAL", "user-login", None, None, False),
    ("FINANCE", "transfer-money", (500_000, 5_000_000), "account", False),
    ("FINANCE", "pay-bill", (100_000, 1_000_000), "account", False),
    ("STOCK_TRADING", "view-portfolio", None, None, False),
    ("FEEDBACK", "submit-nps-form", None, None, False),
]
# Anonymous (no resolved profile yet) events for the two domains not
# otherwise represented in the AppsFlyer-only CIR demo dataset.
UNRESOLVED_EVENTS = [
    ("travel", "TRAVEL", "search-flight", None, None, False),
    ("travel", "TRAVEL", "booking", (1_000_000, 8_000_000), "booking", True),
    ("real_estate", "REAL_ESTATE", "view-property", None, "property", False),
    ("real_estate", "REAL_ESTATE", "request-property-tour", None, "property", False),
]


def seed_raw_events(cursor, master_profiles: list) -> None:
    logger.info("Seeding cdp_raw_events (behavioral events across GENERAL/COMMERCE/FINANCE/STOCK_TRADING/FEEDBACK)...")
    for m in master_profiles:
        rng = stable_rng(f"events:{m['master_profile_id']}")
        catalog = RETAIL_EVENTS if m["domain"] == "retail" else BANKING_EVENTS
        for category, event_name, value_range, entity_type, is_conversion in catalog:
            cursor.execute(
                f"""
                INSERT INTO {_table('cdp_raw_events')}
                    (tenant_id, domain, master_profile_id, source_system, channel, event_category,
                     event_name, is_conversion, entity_type, event_value, currency, event_time)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
                """,
                (
                    DEMO_TENANT_ID, m["domain"], m["master_profile_id"],
                    "AppsFlyer" if m["domain"] == "retail" else "CoreBanking",
                    "mobile_app", category, event_name, is_conversion, entity_type,
                    rng.randint(*value_range) if value_range else None, "VND",
                    datetime.now() - timedelta(days=rng.randint(1, 60), hours=rng.randint(0, 23)),
                ),
            )

    logger.info("Seeding anonymous cdp_raw_events for travel/real_estate domains (no resolved profile yet)...")
    rng = stable_rng("unresolved_events")
    for domain, category, event_name, value_range, entity_type, is_conversion in UNRESOLVED_EVENTS:
        cursor.execute(
            f"""
            INSERT INTO {_table('cdp_raw_events')}
                (tenant_id, domain, device_id, source_system, channel, event_category, event_name,
                 is_conversion, entity_type, event_value, currency, event_time)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s);
            """,
            (
                DEMO_TENANT_ID, domain, f"demo-anon-device-{rng.randint(1000, 9999)}", "WebTracking", "web",
                category, event_name, is_conversion, entity_type,
                rng.randint(*value_range) if value_range else None, "VND",
                datetime.now() - timedelta(days=rng.randint(1, 30)),
            ),
        )


def seed_graph_edges(cursor, crm_ids: dict, master_profiles: list) -> None:
    logger.info("Seeding graph_edges (belongs_to/converted/has/belongs_to_industry/is_connected_to/is_from)...")
    metadata = Json({"demo_tenant": DEMO_TENANT_ID})
    edges = []
    if crm_ids["contact"] and crm_ids["account"]:
        account_id = list(crm_ids["account"].values())[0]
        edges.append(("belongs_to", crm_ids["contact"][0], "crm_contact", account_id, "crm_account"))
        edges.append(("belongs_to_industry", account_id, "crm_account", list(crm_ids["industry"].values())[0], "crm_industry"))
    if crm_ids["lead"] and crm_ids["contact"]:
        edges.append(("converted", crm_ids["lead"][0], "crm_lead", crm_ids["contact"][0], "crm_contact"))
    if crm_ids["lead"] and crm_ids["lead_source"]:
        edges.append(("is_from", crm_ids["lead"][0], "crm_lead", list(crm_ids["lead_source"].values())[0], "crm_lead_source"))
    if crm_ids["account"] and crm_ids["opportunity"]:
        edges.append(("has", list(crm_ids["account"].values())[0], "crm_account", crm_ids["opportunity"][0], "crm_opportunity"))
    if len(master_profiles) >= 2:
        edges.append((
            "is_connected_to", master_profiles[0]["master_profile_id"], "cdp_master_profiles",
            master_profiles[1]["master_profile_id"], "cdp_master_profiles",
        ))

    for relation, from_id, from_type, to_id, to_type in edges:
        cursor.execute(
            f"""
            INSERT INTO {_table('graph_edges')}
                (from_id, to_id, from_type, to_type, relation, description, metadata)
            VALUES (%s, %s, %s, %s, %s, %s, %s);
            """,
            (from_id, to_id, from_type, to_type, relation, f"Demo edge: {from_type} -{relation}-> {to_type}.", metadata),
        )


# crm_contact <-> cdp_master_profiles are two SEPARATE domains in
# database-schema.sql -- crm_contact has no tenant_id/master_profile_id
# column at all, and cdp_master_profiles has nothing pointing back to
# crm_contact. There is NO natural FK/shared key between them (see the
# discussion in the session this was added). The only schema-supported way to
# express "this resolved consumer profile is ALSO this B2B contact/decision-
# maker" is the generic graph_edges table -- which is exactly the join key
# this function seeds, in both directions (a graph_edges row, plus a
# denormalized cross-reference id on each side for quick lookups without a
# graph_edges join).
CONTACT_MASTER_LINK_ACCOUNTS = (
    # (account_name, contact_defs index, domain, master_profiles index within that domain)
    ("Sacombank Digital", 0, "banking", 0),
    ("Techcombank Wealth Partners", 1, "banking", 1),
    ("VinMart Retail Group", 2, "retail", 0),
    ("Saigon Co.op Omnichannel", 3, "retail", 1),
)


def link_crm_contacts_to_master_profiles(cursor, crm_ids: dict, master_profiles: list) -> None:
    """Bridges a handful of ``crm_contact`` rows to real resolved
    ``cdp_master_profiles`` rows -- demonstrating that a B2B decision-maker
    (CRM contact) can ALSO be a resolved individual consumer identity (CIR
    golden record), even though the two tables share no FK.

    Writes the link in three redundant, mutually-consistent ways so it's
    discoverable/joinable regardless of which table you start from:
      1. A ``graph_edges`` row (``relation = 'is_active_as'``,
         ``cdp_master_profiles -> crm_contact``) -- the canonical, generic
         cross-entity relationship record.
      2. ``cdp_master_profiles.attributes->>'linked_crm_contact_id'`` --
         quick lookup from the master-profile side without a graph_edges join.
      3. ``crm_contact.metadata->>'linked_master_profile_id'`` -- quick
         lookup from the CRM-contact side without a graph_edges join.
    """
    banking = [m["master_profile_id"] for m in master_profiles if m["domain"] == "banking"]
    retail = [m["master_profile_id"] for m in master_profiles if m["domain"] == "retail"]
    domain_pools = {"banking": banking, "retail": retail}
    contacts = crm_ids["contact"]

    metadata = Json({"demo_tenant": DEMO_TENANT_ID})
    links = []
    for account_name, contact_index, domain, pool_index in CONTACT_MASTER_LINK_ACCOUNTS:
        pool = domain_pools[domain]
        if contact_index >= len(contacts) or pool_index >= len(pool):
            continue
        links.append((pool[pool_index], contacts[contact_index], account_name))

    if not links:
        logger.warning("Not enough resolved master profiles/CRM contacts to link -- skipping.")
        return

    logger.info("Linking %d crm_contact row(s) to real resolved cdp_master_profiles rows...", len(links))
    for master_id, contact_id, account_name in links:
        cursor.execute(
            f"""
            INSERT INTO {_table('graph_edges')}
                (from_id, to_id, from_type, to_type, relation, description, metadata)
            VALUES (%s, %s, 'cdp_master_profiles', 'crm_contact', 'is_active_as', %s, %s);
            """,
            (
                master_id, contact_id,
                f"This resolved consumer profile is also the B2B contact/decision-maker at {account_name}.",
                metadata,
            ),
        )
        cursor.execute(
            f"""
            UPDATE {_table('cdp_master_profiles')}
            SET attributes = COALESCE(attributes, '{{}}'::jsonb) || %s
            WHERE master_profile_id = %s;
            """,
            (Json({"linked_crm_contact_id": contact_id}), master_id),
        )
        cursor.execute(
            f"""
            UPDATE {_table('crm_contact')}
            SET metadata = COALESCE(metadata, '{{}}'::jsonb) || %s
            WHERE contact_id = %s;
            """,
            (Json({"linked_master_profile_id": master_id}), contact_id),
        )


# --------------------------------------------------------------------------
# 5. cdp_master_profiles enrichment
# --------------------------------------------------------------------------

RETAIL_CHANNELS = ("Mobile App", "Website")
BANKING_CHANNELS = ("Internet Banking App", "Mobile App", "Branch")
LIFECYCLE_STAGES = ("prospect", "lead", "customer", "vip", "dormant", "churn_risk")
OCCUPATIONS = ("engineer", "teacher", "business_owner", "student", "civil_servant", "sales_professional")
INCOME_SEGMENTS = ("low", "medium", "high")
CITIES = ("Ho Chi Minh City", "Hanoi", "Da Nang", "Can Tho")


def _make_persona_summary(domain: str, lifecycle_stage: str, preferred_channel: str, rng: random.Random) -> str:
    flavor = rng.choice(
        [
            "discovered the brand through a paid social campaign",
            "was referred by an existing customer",
            "signed up directly via organic search",
            "engaged first through an offline event",
        ]
    )
    return (
        f"{domain.capitalize()} profile who {flavor}; primarily engages via {preferred_channel}; "
        f"currently in the '{lifecycle_stage}' lifecycle stage."
    )


def enrich_master_profiles(cursor, master_profiles: list) -> None:
    logger.info("Enriching %d master profiles with lifecycle/ML-scoring/domain-specific fields...", len(master_profiles))
    for idx, m in enumerate(master_profiles):
        master_id = m["master_profile_id"]
        domain = m["domain"]
        rng = stable_rng(f"enrich:{master_id}")

        lifecycle_stage = rng.choice(LIFECYCLE_STAGES)
        is_established_customer = lifecycle_stage in ("customer", "vip", "dormant", "churn_risk")
        preferred_channel = rng.choice(RETAIL_CHANNELS if domain == "retail" else BANKING_CHANNELS)
        customer_since = (
            (m["created_at"] - timedelta(days=rng.randint(0, 400))).date() if is_established_customer else None
        )
        last_activity_at = datetime.now() - timedelta(days=rng.randint(0, 30), hours=rng.randint(0, 23))

        num_sources = len(m.get("source_systems") or [])
        identity_confidence_score = min(1.0, round(0.5 + 0.15 * num_sources, 4))
        churn_probability = round(rng.uniform(0, 1), 4)
        churn_risk_tier = (
            "critical" if churn_probability >= 0.85 else
            "high" if churn_probability >= 0.6 else
            "medium" if churn_probability >= 0.3 else "low"
        )
        lead_conversion_probability = round(rng.uniform(0, 1), 4)
        lead_grade = "Hot" if lead_conversion_probability >= 0.7 else "Warm" if lead_conversion_probability >= 0.4 else "Cold"
        historical_clv = round(rng.uniform(500, 5000) * (10 if domain == "banking" else 1), 2)
        predictive_clv = round(historical_clv * rng.uniform(1.0, 1.8), 2)
        clv_segment = "high" if predictive_clv > (30000 if domain == "banking" else 3000) else "medium" if predictive_clv > (10000 if domain == "banking" else 1000) else "low"
        engagement_score = round(rng.uniform(0, 100), 2)
        latest_nps_score = rng.randint(0, 10)
        average_csat = round(rng.uniform(1, 5), 2)
        overall_sentiment_score = round(rng.uniform(-1, 1), 4)
        profile_completeness_score = round(rng.uniform(40, 100), 2)
        segmentation_tags = [domain, lifecycle_stage, clv_segment + "_value"]
        attributes = Json({"occupation": rng.choice(OCCUPATIONS), "income_segment": rng.choice(INCOME_SEGMENTS)})
        model_versions = Json({
            "churn_model": "v1", "clv_model": "v1", "lead_scoring_model": "v1",
            "cx_scoring_model": "v1", "data_quality_model": "v1",
            "identity_resolution_scoring_model": "v1",
        })
        gender = rng.choice(("male", "female", "other"))
        address = Json({"city": rng.choice(CITIES), "country": "VN"})
        profile_picture_url = f"https://api.dicebear.com/7.x/identicon/svg?seed={master_id}"
        persona_summary = _make_persona_summary(domain, lifecycle_stage, preferred_channel, rng)

        set_clauses = [
            "lifecycle_stage = %s", "preferred_channel = %s", "customer_since = %s",
            "last_activity_at = %s", "churn_probability = %s", "churn_risk_tier = %s",
            "lead_conversion_probability = %s", "lead_grade = %s", "historical_clv = %s",
            "predictive_clv = %s", "clv_segment = %s", "engagement_score = %s",
            "latest_nps_score = %s", "average_csat = %s", "overall_sentiment_score = %s",
            "profile_completeness_score = %s", "identity_confidence_score = %s",
            "segmentation_tags = %s", "attributes = COALESCE(attributes, '{}'::jsonb) || %s",
            "model_versions = %s", "scores_updated_at = NOW()", "gender = %s", "address = %s",
            "profile_picture_url = %s", "persona_summary = %s",
            # acquisition_source/acquisition_campaign: genuinely derivable from the
            # raw profile that first created this master, via first_seen_raw_profile_id.
            f"""acquisition_source = COALESCE(acquisition_source, (
                SELECT media_source FROM {_table('cdp_raw_profiles_stage')}
                WHERE raw_profile_id = {_table('cdp_master_profiles')}.first_seen_raw_profile_id
            ))""",
            f"""acquisition_campaign = COALESCE(acquisition_campaign, (
                SELECT campaign FROM {_table('cdp_raw_profiles_stage')}
                WHERE raw_profile_id = {_table('cdp_master_profiles')}.first_seen_raw_profile_id
            ))""",
        ]
        params = [
            lifecycle_stage, preferred_channel, customer_since, last_activity_at,
            churn_probability, churn_risk_tier, lead_conversion_probability, lead_grade,
            historical_clv, predictive_clv, clv_segment, engagement_score, latest_nps_score,
            average_csat, overall_sentiment_score, profile_completeness_score,
            identity_confidence_score, segmentation_tags, attributes, model_versions,
            gender, address, profile_picture_url, persona_summary,
        ]

        if domain == "retail":
            set_clauses += ["loyalty_id = %s", "membership_tier = %s", "preferred_store_code = %s"]
            params += [
                f"LOY-{master_id[:8]}",
                rng.choice(("Silver", "Gold", "Platinum")),
                f"STORE-{rng.randint(1, 20):03d}",
            ]
        else:
            set_clauses += ["cif_number = %s", "account_numbers = %s", "kyc_status = %s", "risk_segment = %s"]
            params += [
                f"CIF{rng.randint(10_000_000, 99_999_999)}",
                [f"{rng.randint(1000000000, 9999999999)}" for _ in range(rng.randint(1, 2))],
                rng.choice(("unverified", "pending", "verified", "rejected")),
                rng.choice(("low", "medium", "high")),
            ]

        if idx < EMBEDDING_PROFILE_LIMIT:
            embedding_rng = stable_rng(f"embedding:{master_id}")
            vector_literal = "[" + ",".join(f"{embedding_rng.uniform(-1, 1):.6f}" for _ in range(PERSONA_EMBEDDING_DIM)) + "]"
            set_clauses.append(f"persona_embedding = %s::vector({PERSONA_EMBEDDING_DIM})")
            params.append(vector_literal)

        params.append(master_id)
        cursor.execute(
            f"UPDATE {_table('cdp_master_profiles')} SET {', '.join(set_clauses)} WHERE master_profile_id = %s;",
            tuple(params),
        )


# --------------------------------------------------------------------------
# Orchestration
# --------------------------------------------------------------------------

def fetch_master_profiles(cursor) -> list:
    cursor.execute(
        f"""
        SELECT master_profile_id, domain, source_systems, first_seen_raw_profile_id, created_at
        FROM {_table('cdp_master_profiles')}
        WHERE tenant_id = %s
        ORDER BY created_at;
        """,
        (DEMO_TENANT_ID,),
    )
    return cursor.fetchall()


def main() -> None:
    conn = psycopg2.connect(host=DB_HOST, dbname=DB_NAME, user=DB_USER, password=DB_PASSWORD, port=DB_PORT)
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cursor:
            master_profiles = fetch_master_profiles(cursor)
            if not master_profiles:
                logger.error(
                    "No resolved master profiles found for tenant_id=%s -- run "
                    "scripts/init_sample_data.py + scripts/run_demo_resolution.py first.",
                    DEMO_TENANT_ID,
                )
                return

            detail_profiles = master_profiles[:DETAIL_PROFILE_LIMIT]

            seed_relation_types(cursor)
            crm_ids = seed_crm_entities(cursor)
            reset_tenant_scoped_demo_tables(cursor)
            seed_relations(cursor, detail_profiles)
            seed_customer_contacts(cursor, detail_profiles)
            seed_transactions(cursor, detail_profiles)
            seed_raw_events(cursor, detail_profiles)
            seed_graph_edges(cursor, crm_ids, detail_profiles)
            enrich_master_profiles(cursor, master_profiles)
            link_crm_contacts_to_master_profiles(cursor, crm_ids, master_profiles)

        conn.commit()
        logger.info(
            "Full demo data seeded: %d master profiles enriched (%d with a persona_embedding), "
            "%d got detail rows (relations/contacts/transactions/events); CRM journey graph + "
            "graph_edges + cdp_relation_types seeded; crm_contact <-> cdp_master_profiles linked "
            "via graph_edges ('is_active_as') + cross-referenced attributes/metadata.",
            len(master_profiles), min(len(master_profiles), EMBEDDING_PROFILE_LIMIT), len(detail_profiles),
        )
    except Exception:
        conn.rollback()
        logger.exception("Failed to seed full demo data.")
        raise
    finally:
        conn.close()


if __name__ == "__main__":
    main()
