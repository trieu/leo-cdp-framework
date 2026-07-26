"""Unit tests for the generic CRUD router factory's tenant_id filter wiring
(core.routers._generic.build_crud_router) -- the "multi tenant" list/count
query parameter that backs tenant-scoped browsing across every crm_*/cdp_*
entity, without needing a real PostgreSQL instance.
"""

import unittest
import uuid
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from core.database import get_db
from core.models.crm import Lead
from core.models.relations import RelationType
from core.routers._generic import build_crud_router
from core.schemas.crm import LeadCreate, LeadRead, LeadUpdate
from core.schemas.relations import RelationTypeCreate, RelationTypeRead, RelationTypeUpdate


class FakeCRUD:
    """Stands in for core.crud.base.CRUDBase, recording the filters it was
    called with instead of touching a real database."""

    last_list_kwargs: dict = {}
    last_count_kwargs: dict = {}

    def __init__(self, model):
        self.model = model

    def list(self, db, *, skip=0, limit=100, **filters):
        FakeCRUD.last_list_kwargs = filters
        return []

    def count(self, db, **filters):
        FakeCRUD.last_count_kwargs = filters
        return 0

    def get(self, db, pk):
        return None


def _build_test_app(model, create_schema, update_schema, read_schema, prefix):
    with patch("core.routers._generic.CRUDBase", FakeCRUD):
        router = build_crud_router(
            model=model,
            pk_field="id",
            pk_type=str,
            create_schema=create_schema,
            update_schema=update_schema,
            read_schema=read_schema,
            prefix=prefix,
            tags=["test"],
        )
    app = FastAPI()
    app.include_router(router)
    app.dependency_overrides[get_db] = lambda: None
    return app


class TenantScopedRouterTests(unittest.TestCase):
    def setUp(self):
        # Avoid any real Redis connection attempts from @cache_response.
        self._cache_patcher = patch("core.cache.get_redis_client", return_value=None)
        self._cache_patcher.start()
        self.addCleanup(self._cache_patcher.stop)

    def test_list_endpoint_passes_tenant_id_filter_for_tenant_scoped_model(self):
        app = _build_test_app(Lead, LeadCreate, LeadUpdate, LeadRead, "/leads")
        client = TestClient(app)
        tenant_id = str(uuid.uuid4())

        response = client.get(f"/leads/?tenant_id={tenant_id}")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(FakeCRUD.last_list_kwargs, {"tenant_id": uuid.UUID(tenant_id)})

    def test_list_endpoint_without_tenant_id_filters_by_none(self):
        app = _build_test_app(Lead, LeadCreate, LeadUpdate, LeadRead, "/leads")
        client = TestClient(app)

        response = client.get("/leads/")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(FakeCRUD.last_list_kwargs, {"tenant_id": None})

    def test_count_endpoint_passes_tenant_id_filter(self):
        app = _build_test_app(Lead, LeadCreate, LeadUpdate, LeadRead, "/leads")
        client = TestClient(app)
        tenant_id = str(uuid.uuid4())

        response = client.get(f"/leads/count?tenant_id={tenant_id}")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(FakeCRUD.last_count_kwargs, {"tenant_id": uuid.UUID(tenant_id)})

    def test_two_different_tenants_produce_two_different_filters(self):
        app = _build_test_app(Lead, LeadCreate, LeadUpdate, LeadRead, "/leads")
        client = TestClient(app)
        tenant_a = str(uuid.uuid4())
        tenant_b = str(uuid.uuid4())

        client.get(f"/leads/?tenant_id={tenant_a}")
        filters_a = dict(FakeCRUD.last_list_kwargs)
        client.get(f"/leads/?tenant_id={tenant_b}")
        filters_b = dict(FakeCRUD.last_list_kwargs)

        self.assertNotEqual(filters_a, filters_b)
        self.assertEqual(filters_a["tenant_id"], uuid.UUID(tenant_a))
        self.assertEqual(filters_b["tenant_id"], uuid.UUID(tenant_b))

    def test_model_without_tenant_id_column_gets_no_tenant_filter(self):
        """RelationType is a global lookup dictionary (no tenant_id column) --
        the generic router must not attempt to filter on it."""
        app = _build_test_app(
            RelationType, RelationTypeCreate, RelationTypeUpdate, RelationTypeRead, "/relation-types"
        )
        client = TestClient(app)

        response = client.get("/relation-types/?tenant_id=00000000-0000-0000-0000-000000000000")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(FakeCRUD.last_list_kwargs, {})


if __name__ == "__main__":
    unittest.main()
