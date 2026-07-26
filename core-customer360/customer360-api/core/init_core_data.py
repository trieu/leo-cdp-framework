

"""Startup-time seed/init data for the Customer 360 API.

Currently seeds a small set of default segmentation tags (``cdp_segments``)
for every tenant that doesn't have any yet, so a fresh install already has a
usable Audience Builder starting point instead of an empty segment list.

Called once from ``app.py``'s startup eve-nt. Safe to call on every app
startup: it's idempotent (skips tenants that already have >= 1 segment, and
the ``(tenant_id, segment_tag)`` unique constraint on ``cdp_segments`` is a
second safety net against duplicate inserts under concurrent startups).
"""

import logging

from sqlalchemy import func, select, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from core.config import settings
from core.database import SessionLocal
from core.models.segmentation import CdpSegment

logger = logging.getLogger(__name__)

# System-default segments seeded for every new tenant. json_rules mirrors the
# jQuery QueryBuilder rule tree an admin would build in the UI; sql_rules is
# the equivalent translated WHERE-clause fragment against cdp_master_profiles.
DEFAULT_SEGMENTS: list[dict] = [
    {
        "segment_tag": "new_customer",
        "segment_name": "New Customers",
        "description": "Profiles that became a paying customer in the last 30 days.",
        "json_rules": {
            "condition": "AND",
            "rules": [{"field": "customer_since", "operator": "greater_or_equal", "value": "-30 days"}],
        },
        "sql_rules": "customer_since >= (CURRENT_DATE - INTERVAL '30 days')",
    },
    {
        "segment_tag": "high_value",
        "segment_name": "High-Value Customers",
        "description": "Profiles with predictive customer lifetime value above 1000.",
        "json_rules": {
            "condition": "AND",
            "rules": [{"field": "predictive_clv", "operator": "greater", "value": 1000}],
        },
        "sql_rules": "predictive_clv > 1000",
    },
    {
        "segment_tag": "churn_risk",
        "segment_name": "At Risk of Churn",
        "description": "Profiles with a high or critical churn risk tier.",
        "json_rules": {
            "condition": "AND",
            "rules": [{"field": "churn_risk_tier", "operator": "in", "value": ["high", "critical"]}],
        },
        "sql_rules": "churn_risk_tier IN ('high', 'critical')",
    },
    {
        "segment_tag": "dormant",
        "segment_name": "Dormant Profiles",
        "description": "Profiles with no activity in the last 90 days.",
        "json_rules": {
            "condition": "AND",
            "rules": [{"field": "last_activity_at", "operator": "less", "value": "-90 days"}],
        },
        "sql_rules": "last_activity_at < (now() - INTERVAL '90 days')",
    },
]


def _final_generated_sql(sql_rules: str) -> str:
    return (
        f"SELECT master_profile_id FROM {settings.db_schema}.cdp_master_profiles "
        f"WHERE tenant_id = :tenant_id AND ({sql_rules})"
    )


def seed_default_segments(db: Session) -> int:
    """Ensures every tenant in ``sys_tenant`` has ``DEFAULT_SEGMENTS`` in
    ``cdp_segments`` (tenants that already have at least one segment are left
    untouched). Returns the number of segment rows inserted."""
    tenant_ids = [row[0] for row in db.execute(text(f"SELECT tenant_id FROM {settings.db_schema}.sys_tenant")).all()]

    inserted = 0
    for tenant_id in tenant_ids:
        # Scope this connection to the tenant being seeded before touching
        # any tenant-scoped/RLS-protected table -- same pattern as
        # identity-resolution-service's per-row set_config (see resolver.py).
        db.execute(text("SELECT set_config('app.tenant_id', :tenant_id, true)"), {"tenant_id": str(tenant_id)})

        existing_count = db.execute(
            select(func.count()).select_from(CdpSegment).where(CdpSegment.tenant_id == tenant_id)
        ).scalar_one()
        if existing_count > 0:
            continue

        for seg in DEFAULT_SEGMENTS:
            db.add(
                CdpSegment(
                    tenant_id=tenant_id,
                    segment_tag=seg["segment_tag"],
                    segment_name=seg["segment_name"],
                    description=seg["description"],
                    json_rules=seg["json_rules"],
                    sql_rules=seg["sql_rules"],
                    final_generated_sql=_final_generated_sql(seg["sql_rules"]),
                    processed_by="human",
                )
            )
        try:
            db.commit()
            inserted += len(DEFAULT_SEGMENTS)
        except IntegrityError:
            # Another worker/process seeded this tenant concurrently -- safe to skip.
            db.rollback()
            logger.info("Default segments already seeded for tenant %s (concurrent init), skipping.", tenant_id)

    return inserted


def init_core_data() -> None:
    """Runs all startup-time seed/init steps for the API.

    Called during the application startup event so all necessary data is in
    place before the app starts serving requests. Failures are logged and
    swallowed rather than raised, so a seeding issue never prevents the API
    itself from starting.
    """
    logger.info("Initializing core data...")
    db = SessionLocal()
    try:
        inserted = seed_default_segments(db)
        if inserted:
            logger.info("Seeded %d default cdp_segments row(s) across tenant(s).", inserted)
    except Exception:
        logger.exception("init_core_data failed (continuing startup without seed data)")
    finally:
        db.close()
    logger.info("Core data initialization complete.")
