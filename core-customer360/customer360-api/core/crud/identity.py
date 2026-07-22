"""CIR-specific aggregate/reporting queries.

Per-row CRUD for cdp_master_profiles / cdp_raw_profiles_stage /
cdp_profile_links / cdp_profile_attributes is handled by the generic
CRUDBase (core/crud/base.py); this module only holds the aggregate queries
used by core/routers/reporting.py, mirroring the "Phân tích & Báo cáo"
section of core-customer360/identity-resolution.md.
"""

import uuid
from typing import Optional

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from core.models.identity import CdpMasterProfile, CdpProfileLink, CdpRawProfileStage

STATUS_CODE_LABELS = {
    3: "processed",
    2: "in_progress",
    1: "new",
    0: "inactive",
    -1: "deleted",
}


def _filter_tenant(stmt, model, tenant_id: Optional[uuid.UUID]):
    if tenant_id is not None:
        stmt = stmt.where(model.tenant_id == tenant_id)
    return stmt


def count_raw_profiles(db: Session, tenant_id: Optional[uuid.UUID] = None) -> int:
    stmt = _filter_tenant(select(func.count()).select_from(CdpRawProfileStage), CdpRawProfileStage, tenant_id)
    return db.execute(stmt).scalar_one()


def count_master_profiles(db: Session, tenant_id: Optional[uuid.UUID] = None) -> int:
    stmt = _filter_tenant(select(func.count()).select_from(CdpMasterProfile), CdpMasterProfile, tenant_id)
    return db.execute(stmt).scalar_one()


def raw_profiles_by_status(db: Session, tenant_id: Optional[uuid.UUID] = None) -> list[dict]:
    stmt = select(CdpRawProfileStage.status_code, func.count().label("count")).group_by(
        CdpRawProfileStage.status_code
    )
    stmt = _filter_tenant(stmt, CdpRawProfileStage, tenant_id)
    rows = db.execute(stmt).all()
    return [
        {"status_code": code, "label": STATUS_CODE_LABELS.get(code, "unknown"), "count": count}
        for code, count in rows
    ]


def raw_profiles_by_domain(db: Session, tenant_id: Optional[uuid.UUID] = None) -> list[dict]:
    stmt = select(CdpRawProfileStage.domain, func.count().label("count")).group_by(CdpRawProfileStage.domain)
    stmt = _filter_tenant(stmt, CdpRawProfileStage, tenant_id)
    return [{"domain": domain, "count": count} for domain, count in db.execute(stmt).all()]


def master_profiles_by_domain(db: Session, tenant_id: Optional[uuid.UUID] = None) -> list[dict]:
    stmt = select(CdpMasterProfile.domain, func.count().label("count")).group_by(CdpMasterProfile.domain)
    stmt = _filter_tenant(stmt, CdpMasterProfile, tenant_id)
    return [{"domain": domain, "count": count} for domain, count in db.execute(stmt).all()]


def raw_profiles_by_source_system(db: Session, tenant_id: Optional[uuid.UUID] = None) -> list[dict]:
    stmt = select(
        CdpRawProfileStage.source_system, CdpRawProfileStage.domain, func.count().label("count")
    ).group_by(CdpRawProfileStage.source_system, CdpRawProfileStage.domain)
    stmt = _filter_tenant(stmt, CdpRawProfileStage, tenant_id)
    return [{"source_system": s, "domain": d, "count": c} for s, d, c in db.execute(stmt).all()]


def count_duplicate_master_profiles(db: Session, tenant_id: Optional[uuid.UUID] = None) -> int:
    """Counts master profiles linked to 2+ raw profiles (i.e. identity
    resolution actually merged multiple source records together)."""
    link_counts = select(CdpProfileLink.master_profile_id, func.count().label("link_count")).group_by(
        CdpProfileLink.master_profile_id
    )
    link_counts = _filter_tenant(link_counts, CdpProfileLink, tenant_id)
    subq = link_counts.subquery()
    stmt = select(func.count()).select_from(subq).where(subq.c.link_count > 1)
    return db.execute(stmt).scalar_one()


def list_duplicate_master_profiles(
    db: Session, tenant_id: Optional[uuid.UUID] = None, skip: int = 0, limit: int = 100
) -> list[dict]:
    """Lists master profiles that consolidated 2+ raw profiles, most-merged first."""
    link_count_subq = (
        select(CdpProfileLink.master_profile_id, func.count().label("link_count"))
        .group_by(CdpProfileLink.master_profile_id)
        .subquery()
    )
    stmt = (
        select(
            CdpMasterProfile.master_profile_id,
            CdpMasterProfile.domain,
            CdpMasterProfile.full_name,
            CdpMasterProfile.source_systems,
            link_count_subq.c.link_count,
        )
        .join(link_count_subq, link_count_subq.c.master_profile_id == CdpMasterProfile.master_profile_id)
        .where(link_count_subq.c.link_count > 1)
        .order_by(link_count_subq.c.link_count.desc())
        .offset(skip)
        .limit(limit)
    )
    stmt = _filter_tenant(stmt, CdpMasterProfile, tenant_id)
    rows = db.execute(stmt).all()
    return [
        {
            "master_profile_id": row.master_profile_id,
            "domain": row.domain,
            "full_name": row.full_name,
            "linked_raw_profile_count": row.link_count,
            "source_systems": row.source_systems,
        }
        for row in rows
    ]


def identity_graph_coverage(db: Session, tenant_id: Optional[uuid.UUID] = None) -> dict:
    """Counts how many master profiles have each identity channel populated."""
    total = count_master_profiles(db, tenant_id)

    def _count(condition) -> int:
        stmt = _filter_tenant(
            select(func.count()).select_from(CdpMasterProfile).where(condition), CdpMasterProfile, tenant_id
        )
        return db.execute(stmt).scalar_one()

    return {
        "total_master_profiles": total,
        "with_email": _count(CdpMasterProfile.email.isnot(None)),
        "with_phone_number": _count(CdpMasterProfile.phone_number.isnot(None)),
        "with_device_id": _count(func.cardinality(CdpMasterProfile.device_ids) > 0),
        "with_advertising_id": _count(func.cardinality(CdpMasterProfile.advertising_ids) > 0),
        "with_cookie_id": _count(func.cardinality(CdpMasterProfile.cookie_ids) > 0),
        "with_external_id": _count(CdpMasterProfile.external_ids != {}),
        "with_national_id": _count(CdpMasterProfile.national_id.isnot(None)),
    }
