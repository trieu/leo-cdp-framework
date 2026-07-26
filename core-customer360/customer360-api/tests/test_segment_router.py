"""Unit tests for the /segments CRUD endpoints (core.routers.segment), built
on the generic CRUD router factory (core.routers._generic.build_crud_router)
-- verifies request/response wiring for CdpSegment (create/list/get/update/
delete/count) entirely against an in-memory fake CRUD layer, no real
PostgreSQL instance required.
"""

import unittest
import uuid
from types import SimpleNamespace
from typing import Any, Optional
from unittest.mock import patch

from fastapi import FastAPI
from fastapi.testclient import TestClient

from core.database import get_db
from core.models.segmentation import CdpSegment
from core.routers._generic import build_crud_router
from core.schemas.segmentation import SegmentCreate, SegmentRead, SegmentUpdate


class FakeSegmentCRUD:
    """Stands in for core.crud.base.CRUDBase(CdpSegment): an in-memory
    dict-backed store instead of a real database, so the router's HTTP-level
    wiring (status codes, request/response schemas, filters) can be tested
    without SQLAlchemy/PostgreSQL."""

    store: dict[uuid.UUID, SimpleNamespace] = {}
    last_list_kwargs: dict[str, Any] = {}
    last_count_kwargs: dict[str, Any] = {}

    def __init__(self, model):
        self.model = model

    @classmethod
    def reset(cls):
        cls.store = {}
        cls.last_list_kwargs = {}
        cls.last_count_kwargs = {}

    def list(self, db, *, skip=0, limit=100, **filters):
        FakeSegmentCRUD.last_list_kwargs = filters
        return list(FakeSegmentCRUD.store.values())

    def count(self, db, **filters):
        FakeSegmentCRUD.last_count_kwargs = filters
        return len(FakeSegmentCRUD.store)

    def get(self, db, pk: uuid.UUID) -> Optional[SimpleNamespace]:
        return FakeSegmentCRUD.store.get(pk)

    def create(self, db, obj_in: dict[str, Any]) -> SimpleNamespace:
        obj = SimpleNamespace(
            segment_id=uuid.uuid4(),
            status_code=1,
            member_count=0,
            last_computed_at=None,
            created_at=None,
            updated_at=None,
            **obj_in,
        )
        FakeSegmentCRUD.store[obj.segment_id] = obj
        return obj

    def update(self, db, db_obj: SimpleNamespace, obj_in: dict[str, Any]) -> SimpleNamespace:
        for field, value in obj_in.items():
            setattr(db_obj, field, value)
        return db_obj

    def delete(self, db, db_obj: SimpleNamespace) -> None:
        FakeSegmentCRUD.store.pop(db_obj.segment_id, None)


def _build_test_app() -> FastAPI:
    with patch("core.routers._generic.CRUDBase", FakeSegmentCRUD):
        router = build_crud_router(
            model=CdpSegment,
            pk_field="segment_id",
            pk_type=uuid.UUID,
            create_schema=SegmentCreate,
            update_schema=SegmentUpdate,
            read_schema=SegmentRead,
            prefix="/segments",
            tags=["Segmentation"],
        )
    app = FastAPI()
    app.include_router(router)
    app.dependency_overrides[get_db] = lambda: None
    return app


def _segment_payload(**overrides) -> dict[str, Any]:
    payload = {
        "tenant_id": str(uuid.uuid4()),
        "domain": "retail",
        "segment_tag": "gen_z_shopper",
        "segment_name": "Gen Z Shoppers",
        "description": "Profiles under 25 with 3+ purchases in the last quarter.",
        "json_rules": {"condition": "AND", "rules": [{"field": "age", "operator": "less", "value": 25}]},
        "sql_rules": "age < 25",
        "processed_by": "human",
    }
    payload.update(overrides)
    return payload


class SegmentCrudTests(unittest.TestCase):
    def setUp(self):
        FakeSegmentCRUD.reset()
        # Avoid any real Redis connection attempts from @cache_response.
        self._cache_patcher = patch("core.cache.get_redis_client", return_value=None)
        self._cache_patcher.start()
        self.addCleanup(self._cache_patcher.stop)
        self.client = TestClient(_build_test_app())

    def test_create_segment_returns_201_with_generated_id(self):
        response = self.client.post("/segments/", json=_segment_payload())

        self.assertEqual(response.status_code, 201)
        body = response.json()
        self.assertIn("segment_id", body)
        self.assertEqual(body["segment_tag"], "gen_z_shopper")
        self.assertEqual(body["segment_name"], "Gen Z Shoppers")
        self.assertEqual(body["processed_by"], "human")
        self.assertEqual(body["status_code"], 1)

    def test_create_segment_defaults_processed_by_to_human(self):
        payload = _segment_payload()
        del payload["processed_by"]

        response = self.client.post("/segments/", json=payload)

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["processed_by"], "human")

    def test_create_segment_accepts_ai_agent_as_processed_by(self):
        response = self.client.post("/segments/", json=_segment_payload(processed_by="ai_agent"))

        self.assertEqual(response.status_code, 201)
        self.assertEqual(response.json()["processed_by"], "ai_agent")

    def test_create_segment_rejects_invalid_processed_by(self):
        response = self.client.post("/segments/", json=_segment_payload(processed_by="robot"))

        self.assertEqual(response.status_code, 422)

    def test_create_segment_rejects_invalid_domain(self):
        response = self.client.post("/segments/", json=_segment_payload(domain="not_a_domain"))

        self.assertEqual(response.status_code, 422)

    def test_get_segment_by_id_after_create(self):
        created = self.client.post("/segments/", json=_segment_payload()).json()

        response = self.client.get(f"/segments/{created['segment_id']}")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["segment_id"], created["segment_id"])

    def test_get_missing_segment_returns_404(self):
        response = self.client.get(f"/segments/{uuid.uuid4()}")

        self.assertEqual(response.status_code, 404)

    def test_list_segments_returns_all_created(self):
        self.client.post("/segments/", json=_segment_payload(segment_tag="tag_a"))
        self.client.post("/segments/", json=_segment_payload(segment_tag="tag_b"))

        response = self.client.get("/segments/")

        self.assertEqual(response.status_code, 200)
        tags = {item["segment_tag"] for item in response.json()}
        self.assertEqual(tags, {"tag_a", "tag_b"})

    def test_list_segments_passes_through_tenant_id_filter(self):
        tenant_id = str(uuid.uuid4())

        response = self.client.get(f"/segments/?tenant_id={tenant_id}")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(FakeSegmentCRUD.last_list_kwargs, {"tenant_id": uuid.UUID(tenant_id)})

    def test_count_segments_reflects_number_created(self):
        self.client.post("/segments/", json=_segment_payload(segment_tag="tag_a"))
        self.client.post("/segments/", json=_segment_payload(segment_tag="tag_b"))

        response = self.client.get("/segments/count")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"count": 2})

    def test_update_segment_partially_changes_only_given_fields(self):
        created = self.client.post("/segments/", json=_segment_payload()).json()

        response = self.client.patch(
            f"/segments/{created['segment_id']}",
            json={"segment_name": "Updated Name", "is_active": False},
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["segment_name"], "Updated Name")
        self.assertFalse(body["is_active"])
        # Untouched fields must survive the partial update.
        self.assertEqual(body["segment_tag"], "gen_z_shopper")
        self.assertEqual(body["description"], created["description"])

    def test_update_missing_segment_returns_404(self):
        response = self.client.patch(f"/segments/{uuid.uuid4()}", json={"segment_name": "Nope"})

        self.assertEqual(response.status_code, 404)

    def test_delete_segment_removes_it(self):
        created = self.client.post("/segments/", json=_segment_payload()).json()

        delete_response = self.client.delete(f"/segments/{created['segment_id']}")
        get_response = self.client.get(f"/segments/{created['segment_id']}")

        self.assertEqual(delete_response.status_code, 204)
        self.assertEqual(get_response.status_code, 404)

    def test_delete_missing_segment_returns_404(self):
        response = self.client.delete(f"/segments/{uuid.uuid4()}")

        self.assertEqual(response.status_code, 404)


if __name__ == "__main__":
    unittest.main()
