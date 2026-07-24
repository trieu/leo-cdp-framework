"""CRM-style entity models: Campaign, Lead, Contact, Account, Opportunity, Industry.

These mirror the ``ENTITY TABLES`` section of core-customer360/database-schema.sql.
The ``embedding vector(1536)`` columns are mapped via pgvector's SQLAlchemy
``Vector`` type for completeness, but are excluded from the default API
response schemas (see core/schemas/crm.py) to keep responses lightweight.
"""

import uuid
from datetime import date, datetime
from typing import Optional

from pgvector.sqlalchemy import Vector
from sqlalchemy import Date, ForeignKey, Numeric, Text, text
from sqlalchemy.dialects.postgresql import ARRAY, JSONB, TIMESTAMP
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column

from core.models.base import Base


class Campaign(Base):
    __tablename__ = "crm_campaign"

    campaign_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    name: Mapped[str] = mapped_column(Text, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    start_date: Mapped[Optional[date]] = mapped_column(Date)
    end_date: Mapped[Optional[date]] = mapped_column(Date)
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)
    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))


class CampaignMember(Base):
    __tablename__ = "crm_campaign_member"

    campaign_member_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    campaign_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("crm_campaign.campaign_id")
    )
    contact_id: Mapped[Optional[uuid.UUID]] = mapped_column(PG_UUID(as_uuid=True))
    status: Mapped[Optional[str]] = mapped_column(Text)
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    joined_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)


class Lead(Base):
    __tablename__ = "crm_lead"

    lead_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    first_name: Mapped[Optional[str]] = mapped_column(Text)
    last_name: Mapped[Optional[str]] = mapped_column(Text)
    email: Mapped[Optional[str]] = mapped_column(Text)
    phone: Mapped[Optional[str]] = mapped_column(Text)
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)


class LeadSource(Base):
    __tablename__ = "crm_lead_source"

    lead_source_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    name: Mapped[str] = mapped_column(Text, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)


class Contact(Base):
    __tablename__ = "crm_contact"

    contact_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    first_name: Mapped[Optional[str]] = mapped_column(Text)
    last_name: Mapped[Optional[str]] = mapped_column(Text)
    email: Mapped[Optional[str]] = mapped_column(Text)
    phone: Mapped[Optional[str]] = mapped_column(Text)
    account_id: Mapped[Optional[uuid.UUID]] = mapped_column(PG_UUID(as_uuid=True))
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)


class Account(Base):
    __tablename__ = "crm_account"

    account_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    name: Mapped[str] = mapped_column(Text, nullable=False)
    industry_id: Mapped[Optional[uuid.UUID]] = mapped_column(PG_UUID(as_uuid=True))
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)


class Opportunity(Base):
    __tablename__ = "crm_opportunity"

    opportunity_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    account_id: Mapped[Optional[uuid.UUID]] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("crm_account.account_id")
    )
    name: Mapped[Optional[str]] = mapped_column(Text)
    value: Mapped[Optional[float]] = mapped_column(Numeric)
    stage: Mapped[Optional[str]] = mapped_column(Text)
    close_date: Mapped[Optional[date]] = mapped_column(Date)
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)


class Industry(Base):
    __tablename__ = "crm_industry"

    industry_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    name: Mapped[str] = mapped_column(Text, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)
