"""Routers for the Customer Identity Resolution (CIR) core models: master
profiles, raw profile staging, profile links, and the matching-rule
metadata / throttle-status tables consumed by identity-resolution-service.
"""

import uuid
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select
from sqlalchemy.orm import Session

from core.config import settings
from core.crud.base import CRUDBase
from core.database import get_db
from core.models.identity import (
    CdpIdResolutionStatus,
    CdpMasterProfile,
    CdpProfileAttribute,
    CdpProfileLink,
    CdpRawProfileStage,
)
from core.routers._generic import build_crud_router
from core.schemas.identity import (
    IdResolutionStatusRead,
    MasterProfileCreate,
    MasterProfileRead,
    MasterProfileUpdate,
    ProfileAttributeCreate,
    ProfileAttributeRead,
    ProfileAttributeUpdate,
    ProfileLinkCreate,
    ProfileLinkRead,
    RawProfileCreate,
    RawProfileRead,
    RawProfileUpdate,
)

# --- Master Profiles ---------------------------------------------------------

master_profiles_router = APIRouter(prefix="/master-profiles", tags=["Identity Resolution - Master Profiles"])
_master_crud = CRUDBase(CdpMasterProfile)


@master_profiles_router.get("/", response_model=list[MasterProfileRead])
def list_master_profiles(
    tenant_id: Optional[uuid.UUID] = None,
    domain: Optional[str] = Query(default=None, pattern="^(retail|banking)$"),
    skip: int = 0,
    limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
    db: Session = Depends(get_db),
):
    return _master_crud.list(db, skip=skip, limit=limit, tenant_id=tenant_id, domain=domain)


@master_profiles_router.get("/count")
def count_master_profiles_endpoint(
    tenant_id: Optional[uuid.UUID] = None,
    domain: Optional[str] = Query(default=None, pattern="^(retail|banking)$"),
    db: Session = Depends(get_db),
):
    return {"count": _master_crud.count(db, tenant_id=tenant_id, domain=domain)}


@master_profiles_router.get("/{master_profile_id}", response_model=MasterProfileRead)
def get_master_profile(master_profile_id: uuid.UUID, db: Session = Depends(get_db)):
    obj = _master_crud.get(db, master_profile_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpMasterProfile '{master_profile_id}' not found")
    return obj


@master_profiles_router.get("/{master_profile_id}/links", response_model=list[ProfileLinkRead])
def get_master_profile_links(master_profile_id: uuid.UUID, db: Session = Depends(get_db)):
    """All raw profiles that were resolved/merged into this master profile."""
    stmt = select(CdpProfileLink).where(CdpProfileLink.master_profile_id == master_profile_id)
    return db.execute(stmt).scalars().all()


@master_profiles_router.post("/", response_model=MasterProfileRead, status_code=201)
def create_master_profile(payload: MasterProfileCreate, db: Session = Depends(get_db)):
    return _master_crud.create(db, payload.model_dump())


@master_profiles_router.patch("/{master_profile_id}", response_model=MasterProfileRead)
def update_master_profile(master_profile_id: uuid.UUID, payload: MasterProfileUpdate, db: Session = Depends(get_db)):
    obj = _master_crud.get(db, master_profile_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpMasterProfile '{master_profile_id}' not found")
    return _master_crud.update(db, obj, payload.model_dump(exclude_unset=True))


@master_profiles_router.delete("/{master_profile_id}", status_code=204)
def delete_master_profile(master_profile_id: uuid.UUID, db: Session = Depends(get_db)):
    obj = _master_crud.get(db, master_profile_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpMasterProfile '{master_profile_id}' not found")
    _master_crud.delete(db, obj)


# --- Raw Profiles Stage -------------------------------------------------------

raw_profiles_router = APIRouter(prefix="/raw-profiles", tags=["Identity Resolution - Raw Profiles"])
_raw_crud = CRUDBase(CdpRawProfileStage)


@raw_profiles_router.get("/", response_model=list[RawProfileRead])
def list_raw_profiles(
    tenant_id: Optional[uuid.UUID] = None,
    domain: Optional[str] = Query(default=None, pattern="^(retail|banking)$"),
    source_system: Optional[str] = None,
    status_code: Optional[int] = None,
    skip: int = 0,
    limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
    db: Session = Depends(get_db),
):
    return _raw_crud.list(
        db,
        skip=skip,
        limit=limit,
        tenant_id=tenant_id,
        domain=domain,
        source_system=source_system,
        status_code=status_code,
    )


@raw_profiles_router.get("/count")
def count_raw_profiles_endpoint(
    tenant_id: Optional[uuid.UUID] = None,
    domain: Optional[str] = Query(default=None, pattern="^(retail|banking)$"),
    source_system: Optional[str] = None,
    status_code: Optional[int] = None,
    db: Session = Depends(get_db),
):
    return {
        "count": _raw_crud.count(
            db, tenant_id=tenant_id, domain=domain, source_system=source_system, status_code=status_code
        )
    }


@raw_profiles_router.get("/{raw_profile_id}", response_model=RawProfileRead)
def get_raw_profile(raw_profile_id: uuid.UUID, db: Session = Depends(get_db)):
    obj = _raw_crud.get(db, raw_profile_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpRawProfileStage '{raw_profile_id}' not found")
    return obj


@raw_profiles_router.post("/", response_model=RawProfileRead, status_code=201)
def create_raw_profile(payload: RawProfileCreate, db: Session = Depends(get_db)):
    """Ingests a raw profile event (status_code defaults to 1 = new/unprocessed,
    ready to be picked up by identity-resolution-service)."""
    return _raw_crud.create(db, payload.model_dump())


@raw_profiles_router.patch("/{raw_profile_id}", response_model=RawProfileRead)
def update_raw_profile(raw_profile_id: uuid.UUID, payload: RawProfileUpdate, db: Session = Depends(get_db)):
    obj = _raw_crud.get(db, raw_profile_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpRawProfileStage '{raw_profile_id}' not found")
    return _raw_crud.update(db, obj, payload.model_dump(exclude_unset=True))


@raw_profiles_router.delete("/{raw_profile_id}", status_code=204)
def delete_raw_profile(raw_profile_id: uuid.UUID, db: Session = Depends(get_db)):
    obj = _raw_crud.get(db, raw_profile_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpRawProfileStage '{raw_profile_id}' not found")
    _raw_crud.delete(db, obj)


# --- Profile Links -------------------------------------------------------------

profile_links_router = APIRouter(prefix="/profile-links", tags=["Identity Resolution - Profile Links"])
_link_crud = CRUDBase(CdpProfileLink)


@profile_links_router.get("/", response_model=list[ProfileLinkRead])
def list_profile_links(
    tenant_id: Optional[uuid.UUID] = None,
    raw_profile_id: Optional[uuid.UUID] = None,
    master_profile_id: Optional[uuid.UUID] = None,
    skip: int = 0,
    limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
    db: Session = Depends(get_db),
):
    return _link_crud.list(
        db,
        skip=skip,
        limit=limit,
        tenant_id=tenant_id,
        raw_profile_id=raw_profile_id,
        master_profile_id=master_profile_id,
    )


@profile_links_router.get("/{link_id}", response_model=ProfileLinkRead)
def get_profile_link(link_id: uuid.UUID, db: Session = Depends(get_db)):
    obj = _link_crud.get(db, link_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpProfileLink '{link_id}' not found")
    return obj


@profile_links_router.post("/", response_model=ProfileLinkRead, status_code=201)
def create_profile_link(payload: ProfileLinkCreate, db: Session = Depends(get_db)):
    return _link_crud.create(db, payload.model_dump())


@profile_links_router.delete("/{link_id}", status_code=204)
def delete_profile_link(link_id: uuid.UUID, db: Session = Depends(get_db)):
    obj = _link_crud.get(db, link_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpProfileLink '{link_id}' not found")
    _link_crud.delete(db, obj)


# --- Profile Attributes (matching-rule metadata) --------------------------------

profile_attributes_router = build_crud_router(
    model=CdpProfileAttribute,
    pk_field="id",
    pk_type=int,
    create_schema=ProfileAttributeCreate,
    update_schema=ProfileAttributeUpdate,
    read_schema=ProfileAttributeRead,
    prefix="/profile-attributes",
    tags=["Identity Resolution - Matching Rules"],
)


# --- Resolution status (real-time throttle state) -------------------------------

resolution_status_router = APIRouter(prefix="/resolution-status", tags=["Identity Resolution - Matching Rules"])


@resolution_status_router.get("/", response_model=IdResolutionStatusRead)
def get_resolution_status(db: Session = Depends(get_db)):
    obj = db.get(CdpIdResolutionStatus, True)
    if obj is None:
        raise HTTPException(
            status_code=404,
            detail="cdp_id_resolution_status has not been initialized yet "
            "(run identity-resolution-service/scripts/init_sample_data.py).",
        )
    return obj


all_identity_routers = [
    master_profiles_router,
    raw_profiles_router,
    profile_links_router,
    profile_attributes_router,
    resolution_status_router,
]
