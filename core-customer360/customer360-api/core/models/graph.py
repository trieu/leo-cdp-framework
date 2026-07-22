"""Graph edges: a generic, partitioned (by ``relation``) knowledge-graph edge
table connecting any two entities (master profiles, campaigns, accounts, ...).

Mirrors the ``GRAPH EDGES`` section of core-customer360/database-schema.sql.
SQLAlchemy maps to the parent table only -- PostgreSQL transparently routes
INSERTs to the correct partition based on the ``relation`` value.
"""

import uuid
from datetime import datetime
from typing import Optional

from pgvector.sqlalchemy import Vector
from sqlalchemy import BigInteger, Text, text
from sqlalchemy.dialects.postgresql import ARRAY, JSONB, TIMESTAMP
from sqlalchemy.dialects.postgresql import UUID as PG_UUID
from sqlalchemy.orm import Mapped, mapped_column

from core.models.base import Base


class GraphEdge(Base):
    __tablename__ = "graph_edges"

    # Composite primary key (edge_id, relation), matching the partitioned
    # table definition. edge_id is still globally unique (BIGSERIAL on the
    # parent), so it's convenient to look up/filter on edge_id alone too.
    edge_id: Mapped[int] = mapped_column(BigInteger, primary_key=True, autoincrement=True)
    relation: Mapped[str] = mapped_column(Text, primary_key=True)

    from_id: Mapped[uuid.UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    to_id: Mapped[uuid.UUID] = mapped_column(PG_UUID(as_uuid=True), nullable=False)
    from_type: Mapped[str] = mapped_column(Text, nullable=False)
    to_type: Mapped[str] = mapped_column(Text, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text)
    keywords: Mapped[Optional[list[str]]] = mapped_column(ARRAY(Text))
    lang: Mapped[Optional[str]] = mapped_column(Text, server_default="en")
    embedding: Mapped[Optional[list[float]]] = mapped_column(Vector(1536))
    metadata_: Mapped[Optional[dict]] = mapped_column("metadata", JSONB)
    created_at: Mapped[Optional[datetime]] = mapped_column(TIMESTAMP(timezone=True), server_default=text("now()"))
