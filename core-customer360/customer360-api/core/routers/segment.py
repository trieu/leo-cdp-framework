"""API for cdp_segments: segmentation/Audience Builder tag metadata, built via
the generic CRUD router factory (see core/routers/_generic.py) since it has a
simple single-column UUID primary key like the CRM entities.
"""

import uuid

from core.models.segmentation import CdpSegment
from core.routers._generic import build_crud_router
from core.schemas.segmentation import SegmentCreate, SegmentRead, SegmentUpdate

segments_router = build_crud_router(
    model=CdpSegment,
    pk_field="segment_id",
    pk_type=uuid.UUID,
    create_schema=SegmentCreate,
    update_schema=SegmentUpdate,
    read_schema=SegmentRead,
    prefix="/segments",
    tags=["Segmentation"],
)

all_segment_routers = [segments_router]
