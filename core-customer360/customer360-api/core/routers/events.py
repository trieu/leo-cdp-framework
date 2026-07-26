"""Read-only API for the high-volume behavioral/transactional event stream
(``cdp_raw_events``). Powers the Customer 360 profile dashboard's timeline /
cross-channel activity widgets. Ingestion happens out-of-band (AppsFlyer/
MoEngage/Web Tracking/CoreBanking/... pipelines writing directly to
Postgres), so this router intentionally only exposes list/count/get.
"""

import uuid
from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from core.cache import cache_response
from core.config import settings
from core.database import get_db
from core.models.events import CdpRawEvent
from core.schemas.events import EventRead

router = APIRouter(prefix="/events", tags=["Behavioral Events"])


@router.get("/", response_model=list[EventRead])
@cache_response("events/list", ttl=settings.cache_ttl_seconds)
def list_events(
    tenant_id: Optional[uuid.UUID] = None,
    master_profile_id: Optional[uuid.UUID] = None,
    domain: Optional[str] = Query(default=None, pattern="^(retail|banking|real_estate|travel)$"),
    channel: Optional[str] = None,
    event_category: Optional[str] = None,
    event_name: Optional[str] = None,
    event_time_from: Optional[datetime] = None,
    event_time_to: Optional[datetime] = None,
    skip: int = 0,
    limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
    db: Session = Depends(get_db),
):
    stmt = select(CdpRawEvent)
    if tenant_id is not None:
        stmt = stmt.where(CdpRawEvent.tenant_id == tenant_id)
    if master_profile_id is not None:
        stmt = stmt.where(CdpRawEvent.master_profile_id == master_profile_id)
    if domain is not None:
        stmt = stmt.where(CdpRawEvent.domain == domain)
    if channel is not None:
        stmt = stmt.where(CdpRawEvent.channel == channel)
    if event_category is not None:
        stmt = stmt.where(CdpRawEvent.event_category == event_category)
    if event_name is not None:
        stmt = stmt.where(CdpRawEvent.event_name == event_name)
    if event_time_from is not None:
        stmt = stmt.where(CdpRawEvent.event_time >= event_time_from)
    if event_time_to is not None:
        stmt = stmt.where(CdpRawEvent.event_time <= event_time_to)
    stmt = stmt.order_by(CdpRawEvent.event_time.desc()).offset(skip).limit(limit)
    return db.execute(stmt).scalars().all()


@router.get("/{event_id}", response_model=EventRead)
@cache_response("events/item", ttl=settings.cache_ttl_seconds)
def get_event(event_id: uuid.UUID, db: Session = Depends(get_db)):
    stmt = select(CdpRawEvent).where(CdpRawEvent.event_id == event_id)
    obj = db.execute(stmt).scalars().first()
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpRawEvent '{event_id}' not found")
    return obj


all_events_routers = [router]
