"""Generic CRUD router factory: builds list/get/create/update/delete
endpoints for a simple entity (single-column primary key) in one call, so
CRM-style entities don't each need hand-written boilerplate endpoints.
"""

import uuid
from typing import Any, Callable, Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session

from core.cache import cache_response, invalidate_prefix
from core.config import settings
from core.crud.base import CRUDBase
from core.database import get_db
from core.models.base import Base


def build_crud_router(
    *,
    model: type[Base],
    pk_field: str,
    pk_type: Callable[[str], Any] = str,
    create_schema: type[BaseModel],
    update_schema: type[BaseModel],
    read_schema: type[BaseModel],
    prefix: str,
    tags: list[str],
) -> APIRouter:
    router = APIRouter(prefix=prefix, tags=tags)
    crud = CRUDBase(model)
    cache_prefix = model.__tablename__
    # Most crm_*/cdp_* models carry a tenant_id column (see database-schema.sql's
    # "ROW LEVEL SECURITY" section); when present, expose it as an optional
    # list/count filter. Actual tenant isolation is enforced at the database
    # layer by the tenant_policy RLS policies (via core/database.py's
    # app.tenant_id session variable) regardless of this filter -- it's a
    # convenience for narrowing results, not a security boundary by itself.
    has_tenant = hasattr(model, "tenant_id")

    @router.get("/", response_model=list[read_schema])
    @cache_response(f"{cache_prefix}/list", ttl=settings.cache_ttl_seconds)
    def list_items(
        skip: int = 0,
        limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
        tenant_id: Optional[uuid.UUID] = None,
        db: Session = Depends(get_db),
    ):
        filters = {"tenant_id": tenant_id} if has_tenant else {}
        return crud.list(db, skip=skip, limit=limit, **filters)

    @router.get("/count")
    @cache_response(f"{cache_prefix}/count", ttl=settings.cache_ttl_seconds)
    def count_items(
        tenant_id: Optional[uuid.UUID] = None,
        db: Session = Depends(get_db),
    ):
        filters = {"tenant_id": tenant_id} if has_tenant else {}
        return {"count": crud.count(db, **filters)}

    @router.get("/{item_id}", response_model=read_schema)
    @cache_response(f"{cache_prefix}/item", ttl=settings.cache_ttl_seconds)
    def get_item(item_id: pk_type, db: Session = Depends(get_db)):
        obj = crud.get(db, item_id)
        if obj is None:
            raise HTTPException(status_code=404, detail=f"{model.__name__} '{item_id}' not found")
        return obj

    @router.post("/", response_model=read_schema, status_code=201)
    def create_item(payload: create_schema, db: Session = Depends(get_db)):
        obj = crud.create(db, payload.model_dump())
        invalidate_prefix(cache_prefix)
        return obj

    @router.patch("/{item_id}", response_model=read_schema)
    def update_item(item_id: pk_type, payload: update_schema, db: Session = Depends(get_db)):
        obj = crud.get(db, item_id)
        if obj is None:
            raise HTTPException(status_code=404, detail=f"{model.__name__} '{item_id}' not found")
        obj = crud.update(db, obj, payload.model_dump(exclude_unset=True))
        invalidate_prefix(cache_prefix)
        return obj

    @router.delete("/{item_id}", status_code=204)
    def delete_item(item_id: pk_type, db: Session = Depends(get_db)):
        obj = crud.get(db, item_id)
        if obj is None:
            raise HTTPException(status_code=404, detail=f"{model.__name__} '{item_id}' not found")
        crud.delete(db, obj)
        invalidate_prefix(cache_prefix)

    return router
