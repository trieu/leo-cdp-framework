"""Pydantic schemas for CdpSegment (see core/models/segmentation.py)."""

import uuid
from datetime import datetime
from typing import Any, Optional

from pydantic import BaseModel, ConfigDict, Field


class SegmentBase(BaseModel):
    tenant_id: uuid.UUID
    user_id: Optional[uuid.UUID] = None
    domain: str = Field(default="all", pattern="^(all|retail|banking|real_estate|travel)$")
    segment_tag: str
    segment_name: str
    description: Optional[str] = None
    json_rules: dict[str, Any] = Field(default_factory=dict)
    sql_rules: Optional[str] = None
    final_generated_sql: Optional[str] = None
    processed_by: str = Field(default="human", pattern="^(human|ai_agent)$")
    is_active: bool = True


class SegmentCreate(SegmentBase):
    pass


class SegmentUpdate(BaseModel):
    user_id: Optional[uuid.UUID] = None
    domain: Optional[str] = Field(default=None, pattern="^(all|retail|banking|real_estate|travel)$")
    segment_tag: Optional[str] = None
    segment_name: Optional[str] = None
    description: Optional[str] = None
    json_rules: Optional[dict[str, Any]] = None
    sql_rules: Optional[str] = None
    final_generated_sql: Optional[str] = None
    processed_by: Optional[str] = Field(default=None, pattern="^(human|ai_agent)$")
    is_active: Optional[bool] = None
    member_count: Optional[int] = None
    last_computed_at: Optional[datetime] = None
    status_code: Optional[int] = None


class SegmentRead(SegmentBase):
    model_config = ConfigDict(from_attributes=True)
    segment_id: uuid.UUID
    member_count: Optional[int] = None
    last_computed_at: Optional[datetime] = None
    status_code: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
