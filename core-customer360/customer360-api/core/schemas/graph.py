"""Pydantic schemas for the generic knowledge-graph edge table."""

import uuid
from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict


class GraphEdgeBase(BaseModel):
    from_id: uuid.UUID
    to_id: uuid.UUID
    from_type: str
    to_type: str
    relation: str
    description: Optional[str] = None
    keywords: Optional[list[str]] = None
    lang: Optional[str] = "en"
    metadata_: Optional[dict] = None


class GraphEdgeCreate(GraphEdgeBase):
    pass


class GraphEdgeRead(GraphEdgeBase):
    model_config = ConfigDict(from_attributes=True)
    edge_id: int
    created_at: Optional[datetime] = None
