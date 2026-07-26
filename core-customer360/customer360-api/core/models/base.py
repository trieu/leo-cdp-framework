"""Shared SQLAlchemy 2 declarative base.

All ORM models share one MetaData bound to the ``customer360`` Postgres
schema (see core/config.py: DB_SCHEMA), so individual models don't need to
repeat ``__table_args__ = {"schema": ...}``.
"""

from sqlalchemy import MetaData
from sqlalchemy.orm import DeclarativeBase

from core.config import settings


class Base(DeclarativeBase):
    metadata = MetaData(schema=settings.db_schema)
