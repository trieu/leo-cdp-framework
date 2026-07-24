"""Customer Identity Resolution (CIR) reporting/analytics API.

Mirrors the ad-hoc SQL queries in the "Phân tích & Báo cáo" section of
core-customer360/identity-resolution.md as proper JSON endpoints.
"""

import uuid
from typing import Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from core.cache import cache_response
from core.config import settings
from core.crud import identity as identity_crud
from core.database import get_db
from core.schemas.reporting import CirSummary, DuplicateMasterProfile, IdentityGraphCoverage

router = APIRouter(prefix="/reporting", tags=["Identity Resolution - Reporting"])


@router.get("/summary", response_model=CirSummary)
@cache_response("reporting/summary", ttl=settings.cache_ttl_seconds)
def get_cir_summary(tenant_id: Optional[uuid.UUID] = None, db: Session = Depends(get_db)):
    """One-shot overview: raw/master profile totals, processing funnel,
    domain/source breakdowns, and duplicate (merged) master profile count."""
    by_status = identity_crud.raw_profiles_by_status(db, tenant_id)
    status_counts = {row["status_code"]: row["count"] for row in by_status}

    return CirSummary(
        total_raw_profiles=identity_crud.count_raw_profiles(db, tenant_id),
        total_master_profiles=identity_crud.count_master_profiles(db, tenant_id),
        processed_raw_profiles=status_counts.get(3, 0),
        pending_raw_profiles=status_counts.get(1, 0),
        in_progress_raw_profiles=status_counts.get(2, 0),
        duplicate_master_profile_count=identity_crud.count_duplicate_master_profiles(db, tenant_id),
        raw_profiles_by_status=by_status,
        raw_profiles_by_domain=identity_crud.raw_profiles_by_domain(db, tenant_id),
        master_profiles_by_domain=identity_crud.master_profiles_by_domain(db, tenant_id),
        raw_profiles_by_source_system=identity_crud.raw_profiles_by_source_system(db, tenant_id),
    )


@router.get("/master-profiles/duplicates", response_model=list[DuplicateMasterProfile])
@cache_response("reporting/duplicates", ttl=settings.cache_ttl_seconds)
def get_duplicate_master_profiles(
    tenant_id: Optional[uuid.UUID] = None,
    skip: int = 0,
    limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
    db: Session = Depends(get_db),
):
    """Master profiles that consolidated 2+ raw profiles -- i.e. identity
    resolution actually merged records from different source systems."""
    return identity_crud.list_duplicate_master_profiles(db, tenant_id, skip=skip, limit=limit)


@router.get("/identity-graph/coverage", response_model=IdentityGraphCoverage)
@cache_response("reporting/coverage", ttl=settings.cache_ttl_seconds)
def get_identity_graph_coverage(tenant_id: Optional[uuid.UUID] = None, db: Session = Depends(get_db)):
    """Adoption of each identity channel (email/phone/device/advertising/cookie/
    external id/national id) across all resolved master profiles."""
    return identity_crud.identity_graph_coverage(db, tenant_id)
