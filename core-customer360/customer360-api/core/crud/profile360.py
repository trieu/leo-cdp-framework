"""Aggregate queries that power the Customer 360 profile dashboard widgets
(engagement summary, cross-channel activity, top interests, unified
timeline) by combining ``cdp_raw_events``, ``crm_transactions`` and
``crm_customer_contacts`` for a single resolved master profile. Kept
separate from core/crud/identity.py (tenant-wide CIR reporting) since these
are all scoped to one master_profile_id.
"""

import uuid
from datetime import datetime, timedelta, timezone
from typing import Optional

from sqlalchemy import text
from sqlalchemy.orm import Session

from core.config import settings

_SCHEMA = settings.db_schema

# Friendly display labels for cdp_raw_events.event_category values.
EVENT_CATEGORY_LABELS = {
    "GENERAL": "General Activity",
    "EDUCATION": "Education",
    "COMMERCE": "Shopping",
    "FEEDBACK": "Feedback",
    "FINANCE": "Finance",
    "STOCK_TRADING": "Investments",
    "TRAVEL": "Travel",
    "REAL_ESTATE": "Real Estate",
    "SERVICE_INDUSTRY": "Service Industry",
}

# Friendly display titles for cdp_raw_events.event_name values.
EVENT_NAME_TITLES = {
    "user-login": "Logged in",
    "page-view": "Viewed a page",
    "search": "Searched",
    "add-to-cart": "Added item to cart",
    "purchase": "Made a purchase",
    "pay-bill": "Paid a bill",
    "transfer-money": "Transferred funds",
    "view-portfolio": "Viewed investment portfolio",
    "submit-nps-form": "Submitted NPS feedback",
    "submit-csat-form": "Submitted CSAT feedback",
    "booking": "Made a booking",
    "search-flight": "Searched for flights",
    "view-property": "Viewed a property listing",
    "request-property-tour": "Requested a property tour",
}


def _window_start(days: int) -> datetime:
    return datetime.now(timezone.utc) - timedelta(days=days)


def get_engagement_summary(
    db: Session, master_profile_id: uuid.UUID, days: int = 90
) -> dict:
    since = _window_start(days)

    logins = db.execute(
        text(
            f"SELECT COUNT(*) FROM {_SCHEMA}.cdp_raw_events "
            f"WHERE master_profile_id = :mpid AND event_name = 'user-login' AND event_time >= :since"
        ),
        {"mpid": str(master_profile_id), "since": since},
    ).scalar_one()

    txn_row = db.execute(
        text(
            f"SELECT COUNT(*) AS cnt, COALESCE(SUM(amount), 0) AS total, "
            f"AVG(amount) AS avg_amount, MAX(currency) AS currency "
            f"FROM {_SCHEMA}.crm_transactions "
            f"WHERE master_profile_id = :mpid AND transaction_time >= :since"
        ),
        {"mpid": str(master_profile_id), "since": since},
    ).mappings().first()

    last_interaction = db.execute(
        text(
            f"""
            SELECT GREATEST(
                (SELECT MAX(event_time) FROM {_SCHEMA}.cdp_raw_events WHERE master_profile_id = :mpid),
                (SELECT MAX(transaction_time) FROM {_SCHEMA}.crm_transactions WHERE master_profile_id = :mpid),
                (SELECT MAX(contact_date) FROM {_SCHEMA}.crm_customer_contacts WHERE master_profile_id = :mpid)
            ) AS last_at
            """
        ),
        {"mpid": str(master_profile_id)},
    ).scalar_one()

    return {
        "period_days": days,
        "total_logins": logins,
        "total_transactions": txn_row["cnt"],
        "total_spent": txn_row["total"],
        "avg_transaction_amount": txn_row["avg_amount"],
        "currency": txn_row["currency"] or "USD",
        "last_interaction_at": last_interaction,
    }


def get_channel_activity(db: Session, master_profile_id: uuid.UUID, days: int = 90) -> dict:
    since = _window_start(days)

    app_sessions = db.execute(
        text(
            f"SELECT COUNT(*) FROM {_SCHEMA}.cdp_raw_events "
            f"WHERE master_profile_id = :mpid AND channel = 'mobile_app' AND event_time >= :since"
        ),
        {"mpid": str(master_profile_id), "since": since},
    ).scalar_one()

    web_sessions = db.execute(
        text(
            f"SELECT COUNT(*) FROM {_SCHEMA}.cdp_raw_events "
            f"WHERE master_profile_id = :mpid AND channel = 'web' AND event_time >= :since"
        ),
        {"mpid": str(master_profile_id), "since": since},
    ).scalar_one()

    customer_service_contacts = db.execute(
        text(
            f"SELECT COUNT(*) FROM {_SCHEMA}.crm_customer_contacts "
            f"WHERE master_profile_id = :mpid AND contact_date >= :since"
        ),
        {"mpid": str(master_profile_id), "since": since},
    ).scalar_one()

    transactions = db.execute(
        text(
            f"SELECT COUNT(*) FROM {_SCHEMA}.crm_transactions "
            f"WHERE master_profile_id = :mpid AND transaction_time >= :since"
        ),
        {"mpid": str(master_profile_id), "since": since},
    ).scalar_one()

    return {
        "period_days": days,
        "app_sessions": app_sessions,
        "web_sessions": web_sessions,
        "customer_service_contacts": customer_service_contacts,
        "transactions": transactions,
    }


def get_top_interests(db: Session, master_profile_id: uuid.UUID, limit: int = 5) -> list[dict]:
    rows = db.execute(
        text(
            f"SELECT event_category, COUNT(*) AS cnt FROM {_SCHEMA}.cdp_raw_events "
            f"WHERE master_profile_id = :mpid GROUP BY event_category ORDER BY cnt DESC LIMIT :limit"
        ),
        {"mpid": str(master_profile_id), "limit": limit},
    ).all()
    if not rows:
        return []
    max_count = max(cnt for _, cnt in rows)
    return [
        {
            "category": category,
            "label": EVENT_CATEGORY_LABELS.get(category, category.replace("_", " ").title()),
            "count": cnt,
            "percentage": round((cnt / max_count) * 100, 1) if max_count else 0.0,
        }
        for category, cnt in rows
    ]


def get_timeline(db: Session, master_profile_id: uuid.UUID, limit: int = 20) -> list[dict]:
    mpid = str(master_profile_id)

    events = db.execute(
        text(
            f"""
            SELECT event_category, event_name, channel, event_value, currency, event_time
            FROM {_SCHEMA}.cdp_raw_events
            WHERE master_profile_id = :mpid
            ORDER BY event_time DESC LIMIT :limit
            """
        ),
        {"mpid": mpid, "limit": limit},
    ).mappings().all()

    transactions = db.execute(
        text(
            f"""
            SELECT transaction_type, entity_name, amount, currency, channel, transaction_time
            FROM {_SCHEMA}.crm_transactions
            WHERE master_profile_id = :mpid
            ORDER BY transaction_time DESC LIMIT :limit
            """
        ),
        {"mpid": mpid, "limit": limit},
    ).mappings().all()

    contacts = db.execute(
        text(
            f"""
            SELECT contact_type, contact_channel, contact_content, contact_date
            FROM {_SCHEMA}.crm_customer_contacts
            WHERE master_profile_id = :mpid
            ORDER BY contact_date DESC LIMIT :limit
            """
        ),
        {"mpid": mpid, "limit": limit},
    ).mappings().all()

    entries: list[dict] = []
    for row in events:
        title = EVENT_NAME_TITLES.get(row["event_name"], row["event_name"].replace("-", " ").title())
        entries.append(
            {
                "kind": "event",
                "title": title,
                "subtitle": EVENT_CATEGORY_LABELS.get(row["event_category"], row["event_category"]),
                "channel": row["channel"],
                "amount": row["event_value"],
                "currency": row["currency"],
                "occurred_at": row["event_time"],
            }
        )
    for row in transactions:
        entries.append(
            {
                "kind": "transaction",
                "title": row["entity_name"] or (row["transaction_type"] or "Transaction").replace("_", " ").title(),
                "subtitle": (row["transaction_type"] or "").replace("_", " ").title(),
                "channel": row["channel"],
                "amount": row["amount"],
                "currency": row["currency"],
                "occurred_at": row["transaction_time"],
            }
        )
    for row in contacts:
        entries.append(
            {
                "kind": "contact",
                "title": (row["contact_type"] or "Contact").replace("_", " ").title(),
                "subtitle": row["contact_content"],
                "channel": row["contact_channel"],
                "amount": None,
                "currency": None,
                "occurred_at": row["contact_date"],
            }
        )

    def _sort_key(entry: dict) -> datetime:
        occurred_at = entry["occurred_at"]
        if occurred_at is None:
            return datetime.min.replace(tzinfo=timezone.utc)
        if occurred_at.tzinfo is None:
            return occurred_at.replace(tzinfo=timezone.utc)
        return occurred_at

    entries.sort(key=_sort_key, reverse=True)
    return entries[:limit]
