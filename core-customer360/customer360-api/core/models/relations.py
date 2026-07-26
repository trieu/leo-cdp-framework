"""Relationship / interaction / transaction models.

Mirrors the ``RELATIONS & EVENTS`` section of
core-customer360/database-schema.sql.
"""

import uuid
from datetime import datetime
from decimal import Decimal
from typing import Optional

from sqlalchemy import ForeignKey, Numeric, Text, UniqueConstraint, text
from sqlalchemy.dialects.postgresql import JSONB, TIMESTAMP
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column

from core.models.base import Base


class RelationType(Base):
    """Dictionary of relation codes usable by CdpRelation (friend, colleague, ...)."""

    __tablename__ = "cdp_relation_types"

    relation_type_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    code: Mapped[str] = mapped_column(Text, unique=True, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text)


class CdpRelation(Base):
    """A directed relationship between two master profiles (e.g. family, referral)."""

    __tablename__ = "cdp_relations"
    __table_args__ = (
        UniqueConstraint(
            "tenant_id",
            "source_master_id",
            "target_master_id",
            "relation_type_id",
            name="cdp_relations_tenant_id_source_master_id_target_master_id_rel_key",
        ),
    )

    relation_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    source_master_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("cdp_master_profiles.master_profile_id"), nullable=False
    )
    target_master_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("cdp_master_profiles.master_profile_id"), nullable=False
    )
    relation_type_id: Mapped[int] = mapped_column(ForeignKey("cdp_relation_types.relation_type_id"), nullable=False)
    created_at: Mapped[Optional[datetime]] = mapped_column(server_default=text("now()"))


class CustomerContact(Base):
    """A logged interaction/touchpoint with a master profile (call, chat, email, ...)."""

    __tablename__ = "crm_customer_contacts"

    contact_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    master_profile_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("cdp_master_profiles.master_profile_id"), nullable=False
    )
    contact_type: Mapped[Optional[str]] = mapped_column(Text)
    contact_channel: Mapped[Optional[str]] = mapped_column(Text)
    contact_content: Mapped[Optional[str]] = mapped_column(Text)
    contact_date: Mapped[Optional[datetime]] = mapped_column(server_default=text("now()"))


class Transaction(Base):
    """A generic financial/retail/travel transaction attributed to a master profile.

    Mirrors ``crm_transactions``: a source-agnostic transaction landing table
    (POS, core banking, booking engines, ...). ``master_profile_id`` is
    nullable/soft-referenced (no NOT NULL) -- same async-backfill pattern as
    ``cdp_raw_events`` -- since a transaction can be ingested before Customer
    Identity Resolution (CIR) has linked it to a resolved profile.
    """

    __tablename__ = "crm_transactions"

    transaction_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    master_profile_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("cdp_master_profiles.master_profile_id")
    )

    source_system: Mapped[Optional[str]] = mapped_column(Text)
    source_transaction_id: Mapped[Optional[str]] = mapped_column(Text)

    transaction_type: Mapped[Optional[str]] = mapped_column(Text)
    transaction_status: Mapped[Optional[str]] = mapped_column(Text)

    entity_type: Mapped[Optional[str]] = mapped_column(Text)
    entity_id: Mapped[Optional[str]] = mapped_column(Text)
    entity_name: Mapped[Optional[str]] = mapped_column(Text)

    quantity: Mapped[Optional[Decimal]] = mapped_column(Numeric(18, 4))
    amount: Mapped[Optional[Decimal]] = mapped_column(Numeric(18, 2))
    currency: Mapped[Optional[str]] = mapped_column(Text)

    channel: Mapped[Optional[str]] = mapped_column(Text)

    merchant_id: Mapped[Optional[str]] = mapped_column(Text)
    merchant_name: Mapped[Optional[str]] = mapped_column(Text)

    location_id: Mapped[Optional[str]] = mapped_column(Text)
    location_name: Mapped[Optional[str]] = mapped_column(Text)

    campaign_id: Mapped[Optional[str]] = mapped_column(Text)
    campaign_name: Mapped[Optional[str]] = mapped_column(Text)

    staff_id: Mapped[Optional[str]] = mapped_column(Text)
    staff_name: Mapped[Optional[str]] = mapped_column(Text)

    transaction_time: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=False))

    attributes: Mapped[Optional[dict]] = mapped_column(JSONB, server_default=text("'{}'::jsonb"))

    imported_at: Mapped[Optional[datetime]] = mapped_column(server_default=text("CURRENT_TIMESTAMP"))
    created_at: Mapped[Optional[datetime]] = mapped_column(server_default=text("CURRENT_TIMESTAMP"))
