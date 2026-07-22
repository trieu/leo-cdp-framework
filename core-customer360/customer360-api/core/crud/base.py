"""Generic CRUD helper, reused by every simple entity router.

Kept intentionally simple (no soft-delete, no complex filtering DSL) --
CIR-specific models (master profiles, raw profiles, reporting) that need
richer queries have their own dedicated module: core/crud/identity.py.
"""

from typing import Any, Generic, Optional, TypeVar

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from core.models.base import Base

ModelType = TypeVar("ModelType", bound=Base)


class CRUDBase(Generic[ModelType]):
    def __init__(self, model: type[ModelType]):
        self.model = model

    def get(self, db: Session, pk: Any) -> Optional[ModelType]:
        return db.get(self.model, pk)

    def list(self, db: Session, *, skip: int = 0, limit: int = 100, **filters: Any) -> list[ModelType]:
        stmt = select(self.model)
        for field, value in filters.items():
            if value is not None:
                stmt = stmt.where(getattr(self.model, field) == value)
        stmt = stmt.offset(skip).limit(limit)
        return list(db.execute(stmt).scalars().all())

    def count(self, db: Session, **filters: Any) -> int:
        stmt = select(func.count()).select_from(self.model)
        for field, value in filters.items():
            if value is not None:
                stmt = stmt.where(getattr(self.model, field) == value)
        return db.execute(stmt).scalar_one()

    def create(self, db: Session, obj_in: dict[str, Any]) -> ModelType:
        db_obj = self.model(**obj_in)
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        return db_obj

    def update(self, db: Session, db_obj: ModelType, obj_in: dict[str, Any]) -> ModelType:
        for field, value in obj_in.items():
            setattr(db_obj, field, value)
        db.add(db_obj)
        db.commit()
        db.refresh(db_obj)
        return db_obj

    def delete(self, db: Session, db_obj: ModelType) -> None:
        db.delete(db_obj)
        db.commit()
