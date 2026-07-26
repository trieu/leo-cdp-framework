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


class _FakeRows:
    """Stands in for a SQLAlchemy CursorResult supporting `.mappings().all()`."""

    def __init__(self, rows: list[dict[str, Any]]):
        self._rows = rows

    def mappings(self):
        return self

    def all(self):
        return self._rows


class _FakeScalarOne:
    """Stands in for a SQLAlchemy CursorResult supporting `.scalar_one()`."""

    def __init__(self, value: Any):
        self._value = value

    def scalar_one(self):
        return self._value


class _FakeExecSession:
    """Minimal Session double recording every execute() call, returning a
    single canned result (this app-level test never issues more than one
    query per request)."""

    def __init__(self, result: Any = None):
        self.result = result
        self.executed: list[tuple[str, Optional[dict[str, Any]]]] = []

    def execute(self, stmt: Any, params: Optional[dict[str, Any]] = None) -> Any:
        self.executed.append((str(stmt), params))
        return self.result


class SegmentMatchedProfilesTests(unittest.TestCase):
    """Tests the real core.routers.segment.segments_router (including the
    hand-written matched-profiles endpoints, not just the generic CRUD
    routes) with a faked CRUD lookup + faked DB session."""

    def setUp(self):
        import core.routers.segment as segment_router_module

        self.segment_router_module = segment_router_module
        self._cache_patcher = patch("core.cache.get_redis_client", return_value=None)
        self._cache_patcher.start()
        self.addCleanup(self._cache_patcher.stop)

        self.app = FastAPI()
        self.app.include_router(segment_router_module.segments_router)

    def _client_for(self, fake_segment: Optional[SimpleNamespace], fake_session: _FakeExecSession) -> TestClient:
        self.app.dependency_overrides[get_db] = lambda: fake_session
        crud_patcher = patch.object(
            self.segment_router_module, "_segment_crud", SimpleNamespace(get=lambda db, pk: fake_segment)
        )
        crud_patcher.start()
        self.addCleanup(crud_patcher.stop)
        return TestClient(self.app)

    def test_matched_profiles_404_for_missing_segment(self):
        client = self._client_for(None, _FakeExecSession())

        response = client.get(f"/segments/{uuid.uuid4()}/matched-profiles")

        self.assertEqual(response.status_code, 404)

    def test_matched_profiles_count_404_for_missing_segment(self):
        client = self._client_for(None, _FakeExecSession())

        response = client.get(f"/segments/{uuid.uuid4()}/matched-profiles/count")

        self.assertEqual(response.status_code, 404)

    def test_matched_profiles_returns_empty_list_when_no_sql_rules(self):
        segment = SimpleNamespace(segment_id=uuid.uuid4(), tenant_id=uuid.uuid4(), sql_rules=None)
        client = self._client_for(segment, _FakeExecSession())

        response = client.get(f"/segments/{segment.segment_id}/matched-profiles")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

    def test_matched_profiles_count_returns_zero_when_no_sql_rules(self):
        segment = SimpleNamespace(segment_id=uuid.uuid4(), tenant_id=uuid.uuid4(), sql_rules=None)
        client = self._client_for(segment, _FakeExecSession())

        response = client.get(f"/segments/{segment.segment_id}/matched-profiles/count")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"count": 0})

    def test_matched_profiles_count_executes_tenant_scoped_query(self):
        tenant_id = uuid.uuid4()
        segment = SimpleNamespace(
            segment_id=uuid.uuid4(),
            tenant_id=tenant_id,
            sql_rules="churn_risk_tier IN ('high', 'critical')",
        )
        fake_session = _FakeExecSession(result=_FakeScalarOne(7))
        client = self._client_for(segment, fake_session)

        response = client.get(f"/segments/{segment.segment_id}/matched-profiles/count")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"count": 7})
        sql, params = fake_session.executed[0]
        self.assertIn("cdp_master_profiles", sql)
        self.assertIn("churn_risk_tier IN ('high', 'critical')", sql)
        self.assertEqual(params["tenant_id"], str(tenant_id))

    def test_matched_profiles_returns_rows_from_query(self):
        tenant_id = uuid.uuid4()
        profile_id = str(uuid.uuid4())
        segment = SimpleNamespace(segment_id=uuid.uuid4(), tenant_id=tenant_id, sql_rules="predictive_clv > 1000")
        row = {
            "master_profile_id": profile_id,
            "tenant_id": str(tenant_id),
            "domain": "retail",
            "is_hashed": False,
            "secondary_emails": [],
            "secondary_phones": [],
            "external_ids": {},
            "device_ids": [],
            "advertising_ids": [],
            "cookie_ids": [],
            "push_tokens": {},
            "account_numbers": [],
            "attributes": {},
            "source_systems": [],
            "model_versions": {},
            "historical_clv": 0.0,
            "status_code": 1,
        }
        fake_session = _FakeExecSession(result=_FakeRows([row]))
        client = self._client_for(segment, fake_session)

        response = client.get(f"/segments/{segment.segment_id}/matched-profiles")

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(len(body), 1)
        self.assertEqual(body[0]["master_profile_id"], profile_id)

    def test_matched_profiles_rejects_unsafe_sql_rules_at_execution_time(self):
        """Defense-in-depth: even if unsafe sql_rules somehow ended up on a
        row (e.g. seeded outside the API), execution-time validation must
        still reject it with a clean 400 rather than running it."""
        segment = SimpleNamespace(
            segment_id=uuid.uuid4(),
            tenant_id=uuid.uuid4(),
            sql_rules="1=1; DROP TABLE cdp_master_profiles;",
        )
        client = self._client_for(segment, _FakeExecSession())

        response = client.get(f"/segments/{segment.segment_id}/matched-profiles")

        self.assertEqual(response.status_code, 400)


if __name__ == "__main__":
    unittest.main()
