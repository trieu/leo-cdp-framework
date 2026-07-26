"""API for cdp_segments: segmentation/Audience Builder tag metadata, built via
the generic CRUD router factory (see core/routers/_generic.py) since it has a
simple single-column UUID primary key like the CRM entities. Also adds a
read-only "matched profiles" endpoint that actually executes a segment's
sql_rules against cdp_master_profiles (see core/utils/sql_safety.py for the
injection-safety validation applied before every execution).
"""

import uuid

from fastapi import Depends, HTTPException, Query
from sqlalchemy import text
from sqlalchemy.orm import Session

from core.cache import cache_response
from core.config import settings
from core.crud.base import CRUDBase
from core.database import get_db
from core.models.segmentation import CdpSegment
from core.routers._generic import build_crud_router
from core.schemas.identity import MasterProfileRead
from core.schemas.segmentation import SegmentCreate, SegmentRead, SegmentUpdate
from core.utils.sql_safety import validate_sql_where_fragment

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

_segment_crud = CRUDBase(CdpSegment)


def _get_segment_or_404(db: Session, segment_id: uuid.UUID) -> CdpSegment:
    segment = _segment_crud.get(db, segment_id)
    if segment is None:
        raise HTTPException(status_code=404, detail=f"CdpSegment '{segment_id}' not found")
    return segment


def _validated_where_fragment(sql_rules: str) -> str:
    """Re-validates sql_rules immediately before execution (defense-in-depth
    against rows written outside the API's own Pydantic validation, e.g.
    core/init_core_data.py's direct ORM inserts) and turns a failure into a
    clean 400 instead of an unhandled 500."""
    try:
        return validate_sql_where_fragment(sql_rules)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@segments_router.get("/{segment_id}/matched-profiles", response_model=list[MasterProfileRead])
@cache_response("segments/matched_profiles", ttl=settings.cache_ttl_seconds)
def get_segment_matched_profiles(
    segment_id: uuid.UUID,
    skip: int = 0,
    limit: int = Query(default=50, le=settings.api_max_page_size),
    db: Session = Depends(get_db),
):
    """Runs the segment's ``sql_rules`` (validated as a safe WHERE-clause
    fragment) against ``cdp_master_profiles``, scoped to the segment's own
    tenant, and returns the currently-matching active profiles."""
    segment = _get_segment_or_404(db, segment_id)
    if not segment.sql_rules:
        return []

    where_fragment = _validated_where_fragment(segment.sql_rules)
    stmt = text(
        f"""
        SELECT * FROM {settings.db_schema}.cdp_master_profiles
        WHERE tenant_id = :tenant_id AND status_code = 1 AND ({where_fragment})
        ORDER BY created_at DESC
        LIMIT :limit OFFSET :skip
        """
    )
    rows = db.execute(stmt, {"tenant_id": str(segment.tenant_id), "limit": limit, "skip": skip}).mappings().all()
    return [dict(row) for row in rows]


@segments_router.get("/{segment_id}/matched-profiles/count")
@cache_response("segments/matched_profiles_count", ttl=settings.cache_ttl_seconds)
def count_segment_matched_profiles(segment_id: uuid.UUID, db: Session = Depends(get_db)):
    """Same matching logic as ``get_segment_matched_profiles`` above, but
    returns just the total count (for pagination / summary display)."""
    segment = _get_segment_or_404(db, segment_id)
    if not segment.sql_rules:
        return {"count": 0}

    where_fragment = _validated_where_fragment(segment.sql_rules)
    stmt = text(
        f"""
        SELECT count(*) FROM {settings.db_schema}.cdp_master_profiles
        WHERE tenant_id = :tenant_id AND status_code = 1 AND ({where_fragment})
        """
    )
    count = db.execute(stmt, {"tenant_id": str(segment.tenant_id)}).scalar_one()
    return {"count": count}


all_segment_routers = [segments_router]
