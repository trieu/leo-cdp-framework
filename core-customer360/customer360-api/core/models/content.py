"""Personalized content items (news / videos / products / articles) shown in
the Customer 360 profile dashboard's "Personalized Items" panel.

New table (not in the original database-schema.sql) -- a lightweight content
library that the ``/content-items/recommended`` endpoint ranks per master
profile by ``segment_tags`` overlap with the profile's ``segmentation_tags``,
so recommendations are computed from real PostgreSQL data rather than
hardcoded in the frontend. See core/routers/content.py.
"""

import uuid
from datetime import datetime
from typing import Optional

from sqlalchemy import ForeignKey, SmallInteger, Text, text
from sqlalchemy.dialects.postgresql import ARRAY, TIMESTAMP
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column

from core.models.base import Base


class CdpContentItem(Base):
    __tablename__ = "cdp_content_items"

    content_item_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(
        PG_UUID(as_uuid=True), ForeignKey("sys_tenant.tenant_id"), nullable=False
    )
    domain: Mapped[str] = mapped_column(Text, nullable=False, server_default="all")
    item_type: Mapped[str] = mapped_column(Text, nullable=False)  # news | video | product | article

    title: Mapped[str] = mapped_column(Text, nullable=False)
    summary: Mapped[Optional[str]] = mapped_column(Text)
    image_url: Mapped[Optional[str]] = mapped_column(Text)
    cta_label: Mapped[Optional[str]] = mapped_column(Text)
    cta_url: Mapped[Optional[str]] = mapped_column(Text)

    segment_tags: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text), server_default=text("ARRAY[]::text[]"))

    published_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
    status_code: Mapped[int] = mapped_column(SmallInteger, server_default="1")

    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
    updated_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
