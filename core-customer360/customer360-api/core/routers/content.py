"""API for personalized content items (news/videos/products/articles) shown
in the Customer 360 profile dashboard, plus a ``/recommended`` endpoint that
ranks items for a given master profile by ``segment_tags`` overlap with that
profile's ``segmentation_tags`` -- computed in PostgreSQL, not hardcoded.
"""

import uuid
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import select, text
from sqlalchemy.orm import Session

from core.cache import cache_response, invalidate_prefix
from core.config import settings
from core.crud.base import CRUDBase
from core.database import get_db
from core.models.content import CdpContentItem
from core.schemas.content import (
    ContentItemCreate,
    ContentItemRead,
    ContentItemUpdate,
    RecommendedContentItem,
)

router = APIRouter(prefix="/content-items", tags=["Personalized Content"])
_crud = CRUDBase(CdpContentItem)


@router.get("/", response_model=list[ContentItemRead])
@cache_response("content_items/list", ttl=settings.cache_ttl_seconds)
def list_content_items(
    tenant_id: Optional[uuid.UUID] = None,
    domain: Optional[str] = Query(default=None, pattern="^(all|retail|banking|real_estate|travel)$"),
    item_type: Optional[str] = Query(default=None, pattern="^(news|video|product|article)$"),
    skip: int = 0,
    limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
    db: Session = Depends(get_db),
):
    return _crud.list(db, skip=skip, limit=limit, tenant_id=tenant_id, domain=domain, item_type=item_type)


@router.get("/recommended", response_model=list[RecommendedContentItem])
@cache_response("content_items/recommended", ttl=settings.cache_ttl_seconds)
def get_recommended_content_items(
    master_profile_id: uuid.UUID,
    item_type: Optional[str] = Query(default=None, pattern="^(news|video|product|article)$"),
    limit: int = Query(default=8, le=50),
    db: Session = Depends(get_db),
):
    """Ranks active content items for ``master_profile_id`` by how many
    ``segment_tags`` overlap with the profile's ``segmentation_tags`` (ties
    broken by most-recently published), falling back to domain-matched
    items with no tag overlap when a profile has few/no tags."""
    profile_row = db.execute(
        text(
            f"SELECT domain, COALESCE(segmentation_tags, ARRAY[]::text[]) AS tags "
            f"FROM {settings.db_schema}.cdp_master_profiles WHERE master_profile_id = :mpid"
        ),
        {"mpid": str(master_profile_id)},
    ).mappings().first()
    if profile_row is None:
        raise HTTPException(status_code=404, detail=f"CdpMasterProfile '{master_profile_id}' not found")

    sql = f"""
        SELECT
            content_item_id, tenant_id, domain, item_type, title, summary, image_url,
            cta_label, cta_url, segment_tags, published_at, status_code, created_at, updated_at,
            ARRAY(SELECT UNNEST(segment_tags) INTERSECT SELECT UNNEST(:tags)) AS matched_tags
        FROM {settings.db_schema}.cdp_content_items
        WHERE status_code = 1
          AND (domain = 'all' OR domain = :domain)
          AND (:item_type IS NULL OR item_type = :item_type)
        ORDER BY cardinality(ARRAY(SELECT UNNEST(segment_tags) INTERSECT SELECT UNNEST(:tags))) DESC,
                 published_at DESC
        LIMIT :limit
    """
    rows = db.execute(
        text(sql),
        {
            "tags": list(profile_row["tags"]),
            "domain": profile_row["domain"],
            "item_type": item_type,
            "limit": limit,
        },
    ).mappings().all()
    return [dict(row) for row in rows]


@router.get("/count")
@cache_response("content_items/count", ttl=settings.cache_ttl_seconds)
def count_content_items(
    tenant_id: Optional[uuid.UUID] = None,
    domain: Optional[str] = Query(default=None, pattern="^(all|retail|banking|real_estate|travel)$"),
    item_type: Optional[str] = Query(default=None, pattern="^(news|video|product|article)$"),
    db: Session = Depends(get_db),
):
    return {"count": _crud.count(db, tenant_id=tenant_id, domain=domain, item_type=item_type)}


@router.get("/{content_item_id}", response_model=ContentItemRead)
@cache_response("content_items/item", ttl=settings.cache_ttl_seconds)
def get_content_item(content_item_id: uuid.UUID, db: Session = Depends(get_db)):
    obj = _crud.get(db, content_item_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpContentItem '{content_item_id}' not found")
    return obj


@router.post("/", response_model=ContentItemRead, status_code=201)
def create_content_item(payload: ContentItemCreate, db: Session = Depends(get_db)):
    obj = _crud.create(db, payload.model_dump())
    invalidate_prefix("content_items")
    return obj


@router.patch("/{content_item_id}", response_model=ContentItemRead)
def update_content_item(content_item_id: uuid.UUID, payload: ContentItemUpdate, db: Session = Depends(get_db)):
    obj = _crud.get(db, content_item_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpContentItem '{content_item_id}' not found")
    obj = _crud.update(db, obj, payload.model_dump(exclude_unset=True))
    invalidate_prefix("content_items")
    return obj


@router.delete("/{content_item_id}", status_code=204)
def delete_content_item(content_item_id: uuid.UUID, db: Session = Depends(get_db)):
    obj = _crud.get(db, content_item_id)
    if obj is None:
        raise HTTPException(status_code=404, detail=f"CdpContentItem '{content_item_id}' not found")
    _crud.delete(db, obj)
    invalidate_prefix("content_items")


all_content_routers = [router]
