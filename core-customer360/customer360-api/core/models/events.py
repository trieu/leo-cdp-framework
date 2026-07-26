"""High-volume behavioral/transactional event fact model.

Mirrors ``cdp_raw_events`` in core-customer360/database-schema.sql: a
range-partitioned (monthly, by event_time) event stream fed by AppsFlyer/
MoEngage/Web Tracking/CoreBanking/POS/... ingestion, used to power the
Customer 360 profile dashboard's engagement summary / cross-channel
activity / timeline / top-interests widgets (see core/crud/profile360.py).

The ORM mapping only needs the logical (event_id, event_time) primary key
that Postgres enforces across all partitions -- SQLAlchemy is unaware of
the partitioning itself, which is transparent at the SQL level.
"""

import uuid
from datetime import datetime
from decimal import Decimal
from typing import Optional

from sqlalchemy import Boolean, ForeignKey, Numeric, Text, text
from sqlalchemy.dialects.postgresql import INET, JSONB, TIMESTAMP
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column

from core.models.base import Base


class CdpRawEvent(Base):
    """A single behavioral/transactional event, optionally linked to a
    resolved master profile (master_profile_id is nullable and backfilled
    asynchronously by CIR, same pattern as cdp_raw_profiles_stage)."""

    __tablename__ = "cdp_raw_events"

    event_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    event_time: Mapped[datetime] = mapped_column(TIMESTAMP(timezone=True), primary_key=True, nullable=False)

    tenant_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("sys_tenant.tenant_id"), nullable=False
    )
    user_id: Mapped[Optional[uuid.UUID]] = mapped_column(PG_UUID(as_uuid=True), ForeignKey("sys_user.user_id"))
    domain: Mapped[str] = mapped_column(Text, nullable=False, server_default="retail")

    master_profile_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("cdp_master_profiles.master_profile_id")
    )
    raw_profile_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("cdp_raw_profiles_stage.raw_profile_id")
    )

    external_customer_id: Mapped[Optional[str]] = mapped_column(Text)
    device_id: Mapped[Optional[str]] = mapped_column(Text)
    advertising_id: Mapped[Optional[str]] = mapped_column(Text)
    cookie_id: Mapped[Optional[str]] = mapped_column(Text)
    session_id: Mapped[Optional[str]] = mapped_column(Text)

    source_system: Mapped[str] = mapped_column(Text, nullable=False)
    channel: Mapped[Optional[str]] = mapped_column(Text)
    platform: Mapped[Optional[str]] = mapped_column(Text)
    ip_address: Mapped[Optional[str]] = mapped_column(INET)
    user_agent: Mapped[Optional[str]] = mapped_column(Text)

    event_category: Mapped[str] = mapped_column(Text, nullable=False, server_default="GENERAL")
    event_name: Mapped[str] = mapped_column(Text, nullable=False)
    is_conversion: Mapped[bool] = mapped_column(Boolean, server_default=text("false"))

    entity_type: Mapped[Optional[str]] = mapped_column(Text)
    entity_id: Mapped[Optional[str]] = mapped_column(Text)

    event_value: Mapped[Optional[Decimal]] = mapped_column(Numeric(15, 2))
    currency: Mapped[Optional[str]] = mapped_column(Text, server_default="USD")

    transaction_id: Mapped[Optional[str]] = mapped_column(Text)
    transaction_status: Mapped[Optional[str]] = mapped_column(Text)

    location_code: Mapped[Optional[str]] = mapped_column(Text)
    location_name: Mapped[Optional[str]] = mapped_column(Text)

    event_payload: Mapped[Optional[dict]] = mapped_column(JSONB)
    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
