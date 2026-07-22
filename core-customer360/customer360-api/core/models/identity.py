"""Customer Identity Resolution (CIR) core models.

Mirrors the ``MASTER PROFILES & IDENTITY RESOLUTION`` section of
core-customer360/database-schema.sql: ``cdp_master_profiles`` (golden
record), ``cdp_raw_profiles_stage`` (AppsFlyer/MoEngage/Web Tracking landing
zone) and ``cdp_profile_links`` (raw -> master links).

``CdpProfileAttribute`` (matching-rule metadata) and
``CdpIdResolutionStatus`` (real-time throttle state) are CIR *runtime*
tables -- they are not part of database-schema.sql and are created
idempotently by identity-resolution-service/scripts/init_sample_data.py
(``CREATE TABLE IF NOT EXISTS``), but are mapped here too since the API
needs full CRUD over the matching-rule configuration.
"""

import uuid
from datetime import date, datetime
from decimal import Decimal
from typing import Optional

from pgvector.sqlalchemy import Vector
from sqlalchemy import Boolean, Date, ForeignKey, Numeric, SmallInteger, Text, UniqueConstraint, text
from sqlalchemy.dialects.postgresql import ARRAY, INET, JSONB, TIMESTAMP
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column

from core.models.base import Base


class CdpMasterProfile(Base):
    """The golden, resolved customer record (one per real-world person/tenant/domain)."""

    __tablename__ = "cdp_master_profiles"

    master_profile_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    domain: Mapped[str] = mapped_column(Text, nullable=False, server_default="retail")

    full_name: Mapped[Optional[str]] = mapped_column(Text)
    first_name: Mapped[Optional[str]] = mapped_column(Text)
    last_name: Mapped[Optional[str]] = mapped_column(Text)
    email: Mapped[Optional[str]] = mapped_column(Text)
    phone_number: Mapped[Optional[str]] = mapped_column(Text)
    date_of_birth: Mapped[Optional[date]] = mapped_column(Date)
    gender: Mapped[Optional[str]] = mapped_column(Text)
    address: Mapped[Optional[dict]] = mapped_column(JSONB)

    external_ids: Mapped[Optional[dict]] = mapped_column(JSONB, server_default=text("'{}'::jsonb"))
    device_ids: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text), server_default=text("ARRAY[]::text[]"))
    advertising_ids: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text), server_default=text("ARRAY[]::text[]"))
    cookie_ids: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text), server_default=text("ARRAY[]::text[]"))
    push_tokens: Mapped[Optional[dict]] = mapped_column(JSONB, server_default=text("'{}'::jsonb"))

    loyalty_id: Mapped[Optional[str]] = mapped_column(Text)
    membership_tier: Mapped[Optional[str]] = mapped_column(Text)
    preferred_store_code: Mapped[Optional[str]] = mapped_column(Text)

    national_id: Mapped[Optional[str]] = mapped_column(Text)
    cif_number: Mapped[Optional[str]] = mapped_column(Text)
    account_numbers: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text), server_default=text("ARRAY[]::text[]"))
    kyc_status: Mapped[Optional[str]] = mapped_column(Text)
    risk_segment: Mapped[Optional[str]] = mapped_column(Text)

    acquisition_source: Mapped[Optional[str]] = mapped_column(Text)
    acquisition_campaign: Mapped[Optional[str]] = mapped_column(Text)
    persona_embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(768))
    segmentation_tags: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    attributes: Mapped[Optional[dict]] = mapped_column(JSONB, server_default=text("'{}'::jsonb"))
    source_systems: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text), server_default=text("ARRAY[]::text[]"))
    first_seen_raw_profile_id: Mapped[Optional[uuid.UUID]] = mapped_column(PG_UUID(as_uuid=True))

    created_at: Mapped[Optional[datetime]] = mapped_column(server_default=text("now()"))
    updated_at: Mapped[Optional[datetime]] = mapped_column(server_default=text("now()"))


class CdpRawProfileStage(Base):
    """Landing zone for inbound AppsFlyer / MoEngage / Web Tracking / CoreBanking / POS events."""

    __tablename__ = "cdp_raw_profiles_stage"

    raw_profile_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    domain: Mapped[str] = mapped_column(Text, nullable=False, server_default="retail")
    source_system: Mapped[str] = mapped_column(Text, nullable=False)
    channel: Mapped[Optional[str]] = mapped_column(Text)

    external_customer_id: Mapped[Optional[str]] = mapped_column(Text)
    full_name: Mapped[Optional[str]] = mapped_column(Text)
    email: Mapped[Optional[str]] = mapped_column(Text)
    phone_number: Mapped[Optional[str]] = mapped_column(Text)
    national_id: Mapped[Optional[str]] = mapped_column(Text)

    device_id: Mapped[Optional[str]] = mapped_column(Text)
    advertising_id: Mapped[Optional[str]] = mapped_column(Text)
    platform: Mapped[Optional[str]] = mapped_column(Text)
    app_version: Mapped[Optional[str]] = mapped_column(Text)
    push_token: Mapped[Optional[str]] = mapped_column(Text)
    cookie_id: Mapped[Optional[str]] = mapped_column(Text)
    ga_client_id: Mapped[Optional[str]] = mapped_column(Text)
    session_id: Mapped[Optional[str]] = mapped_column(Text)
    ip_address: Mapped[Optional[str]] = mapped_column(INET)
    user_agent: Mapped[Optional[str]] = mapped_column(Text)

    media_source: Mapped[Optional[str]] = mapped_column(Text)
    campaign: Mapped[Optional[str]] = mapped_column(Text)
    utm_source: Mapped[Optional[str]] = mapped_column(Text)
    utm_medium: Mapped[Optional[str]] = mapped_column(Text)
    utm_campaign: Mapped[Optional[str]] = mapped_column(Text)

    event_name: Mapped[Optional[str]] = mapped_column(Text)
    event_time: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True))
    event_payload: Mapped[Optional[dict]] = mapped_column(JSONB)

    status_code: Mapped[int] = mapped_column(SmallInteger, server_default="1")
    processed_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True))
    created_at: Mapped[Optional[datetime]] = mapped_column(server_default=text("now()"))


class CdpProfileLink(Base):
    """Links a raw profile to the master profile it was resolved into."""

    __tablename__ = "cdp_profile_links"
    __table_args__ = (
        # Mirrors UNIQUE(tenant_id, raw_profile_id) in database-schema.sql.
        UniqueConstraint("tenant_id", "raw_profile_id", name="cdp_profile_links_tenant_id_raw_profile_id_key"),
    )

    link_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    raw_profile_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("cdp_raw_profiles_stage.raw_profile_id"), nullable=False
    )
    master_profile_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("cdp_master_profiles.master_profile_id"), nullable=False
    )
    match_score: Mapped[Optional[Decimal]] = mapped_column(Numeric(5, 4))
    match_method: Mapped[Optional[str]] = mapped_column(Text)
    created_at: Mapped[Optional[datetime]] = mapped_column(server_default=text("now()"))


class CdpProfileAttribute(Base):
    """Identity-resolution matching-rule metadata consumed by CustomerIdentityResolver."""

    __tablename__ = "cdp_profile_attributes"

    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    attribute_internal_code: Mapped[str] = mapped_column(Text, unique=True, nullable=False)
    name: Mapped[str] = mapped_column(Text, nullable=False)
    status: Mapped[Optional[str]] = mapped_column(Text, server_default="ACTIVE")
    data_type: Mapped[str] = mapped_column(Text, nullable=False, server_default="TEXT")
    is_identity_resolution: Mapped[bool] = mapped_column(Boolean, server_default=text("false"))
    matching_rule: Mapped[Optional[str]] = mapped_column(Text)
    matching_threshold: Mapped[Optional[Decimal]] = mapped_column(Numeric(5, 4))
    consolidation_rule: Mapped[Optional[str]] = mapped_column(Text)
    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))


class CdpIdResolutionStatus(Base):
    """Single-row throttle state for the real-time IdentityResolutionTrigger."""

    __tablename__ = "cdp_id_resolution_status"

    id: Mapped[bool] = mapped_column(Boolean, primary_key=True, server_default=text("true"))
    last_executed_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True))
