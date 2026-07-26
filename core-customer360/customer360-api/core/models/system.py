"""Lightweight stand-in Table objects for ``sys_tenant`` / ``sys_user``.

This API doesn't own tenant/user administration (no full ORM-mapped
entities for them), but every crm_*/cdp_* model's
``ForeignKey("sys_tenant.tenant_id")`` / ``ForeignKey("sys_user.user_id")``
needs *some* ``Table`` registered in the shared ``Base.metadata`` to resolve
those FK targets -- SQLAlchemy sorts tables by FK dependency whenever an ORM
session flushes an INSERT/UPDATE, and raises ``NoReferencedTableError`` if
the referenced table was never declared, even though it's a valid schema
in PostgreSQL itself.

Columns beyond the primary key are intentionally omitted: nothing here ever
queries through these tables, they only need to exist for FK resolution.
"""

from sqlalchemy import Column, Table
from sqlalchemy.dialects.postgresql import UUID as PG_UUID

from core.models.base import Base

sys_tenant_table = Table(
    "sys_tenant",
    Base.metadata,
    Column("tenant_id", PG_UUID(as_uuid=True), primary_key=True),
)

sys_user_table = Table(
    "sys_user",
    Base.metadata,
    Column("user_id", PG_UUID(as_uuid=True), primary_key=True),
)
