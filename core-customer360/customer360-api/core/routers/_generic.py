"""Generic CRUD router factory: builds list/get/create/update/delete
endpoints for a simple entity (single-column primary key) in one call, so
CRM-style entities don't each need hand-written boilerplate endpoints.
"""

from typing import Any, Callable

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session

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

    @router.get("/", response_model=list[read_schema])
    def list_items(
        skip: int = 0,
        limit: int = Query(default=settings.api_default_page_size, le=settings.api_max_page_size),
        db: Session = Depends(get_db),
    ):
        return crud.list(db, skip=skip, limit=limit)

    @router.get("/count")
    def count_items(db: Session = Depends(get_db)):
        return {"count": crud.count(db)}

    @router.get("/{item_id}", response_model=read_schema)
    def get_item(item_id: pk_type, db: Session = Depends(get_db)):
        obj = crud.get(db, item_id)
        if obj is None:
            raise HTTPException(status_code=404, detail=f"{model.__name__} '{item_id}' not found")
        return obj

    @router.post("/", response_model=read_schema, status_code=201)
    def create_item(payload: create_schema, db: Session = Depends(get_db)):
        return crud.create(db, payload.model_dump())

    @router.patch("/{item_id}", response_model=read_schema)
    def update_item(item_id: pk_type, payload: update_schema, db: Session = Depends(get_db)):
        obj = crud.get(db, item_id)
        if obj is None:
            raise HTTPException(status_code=404, detail=f"{model.__name__} '{item_id}' not found")
        return crud.update(db, obj, payload.model_dump(exclude_unset=True))

    @router.delete("/{item_id}", status_code=204)
    def delete_item(item_id: pk_type, db: Session = Depends(get_db)):
        obj = crud.get(db, item_id)
        if obj is None:
            raise HTTPException(status_code=404, detail=f"{model.__name__} '{item_id}' not found")
        crud.delete(db, obj)

    return router
