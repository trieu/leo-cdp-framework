"""Pydantic schemas for CdpRawEvent (see core/models/events.py)."""

import uuid
from datetime import datetime
from decimal import Decimal
from typing import Optional

from pydantic import BaseModel, ConfigDict


class EventRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    event_id: uuid.UUID
    event_time: datetime
    tenant_id: uuid.UUID
    domain: str
    master_profile_id: Optional[uuid.UUID] = None
    raw_profile_id: Optional[uuid.UUID] = None

    external_customer_id: Optional[str] = None
    device_id: Optional[str] = None
    session_id: Optional[str] = None

    source_system: str
    channel: Optional[str] = None
    platform: Optional[str] = None

    event_category: str
    event_name: str
    is_conversion: bool

    entity_type: Optional[str] = None
    entity_id: Optional[str] = None

    event_value: Optional[Decimal] = None
    currency: Optional[str] = None

    transaction_id: Optional[str] = None
    transaction_status: Optional[str] = None

    location_name: Optional[str] = None
    event_payload: Optional[dict] = None
    created_at: Optional[datetime] = None
