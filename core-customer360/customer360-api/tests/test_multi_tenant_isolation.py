"""Unit tests for multi-tenant isolation: the app.tenant_id/app.user_id
Postgres session GUCs (core.database.get_db) that back every tenant_policy
Row-Level Security policy (see database-schema.sql), plus end-to-end
middleware -> request.state -> get_db wiring across concurrent/sequential
requests from *different* tenants.
"""

import unittest
from types import SimpleNamespace
from unittest.mock import patch

from fastapi import FastAPI, Request
from fastapi.testclient import TestClient

from core.auth import auth_middleware
from core.database import get_db
from tests.conftest import FakeDBSession, FakeRedis


def _fake_request(tenant_id=None, user_id=None) -> Request:
    """Builds a minimal object satisfying get_db's `request.state.*` access.

    get_db only ever reads `request.state.tenant_id` / `request.state.user_id`
    via getattr(), so a lightweight SimpleNamespace-based double is enough --
    no real ASGI scope is needed.
    """
    state = SimpleNamespace()
    if tenant_id is not None:
        state.tenant_id = tenant_id
    if user_id is not None:
        state.user_id = user_id
    return SimpleNamespace(state=state)


class GetDbTenantGucTests(unittest.TestCase):
    def test_sets_both_gucs_when_tenant_and_user_present(self):
        session = FakeDBSession()
        request = _fake_request(tenant_id="tenant-123", user_id="user-456")

        with patch("core.database.SessionLocal", return_value=session):
            gen = get_db(request)
            db = next(gen)
            self.assertIs(db, session)
            with self.assertRaises(StopIteration):
                next(gen)

        self.assertEqual(len(session.executed), 2)
        tenant_sql, tenant_params = session.executed[0]
        user_sql, user_params = session.executed[1]
        self.assertIn("app.tenant_id", tenant_sql)
        self.assertEqual(tenant_params["tenant_id"], "tenant-123")
        self.assertIn("app.user_id", user_sql)
        self.assertEqual(user_params["user_id"], "user-456")
        self.assertTrue(session.closed)

    def test_set_config_uses_transaction_local_scope(self):
        """The `true` (is_local) flag matters: it keeps the GUC scoped to the
        current transaction so pooled connections never leak a tenant to the
        next, unrelated request that reuses the same physical connection."""
        session = FakeDBSession()
        request = _fake_request(tenant_id="tenant-abc")

        with patch("core.database.SessionLocal", return_value=session):
            next(get_db(request))

        tenant_sql, _ = session.executed[0]
        self.assertIn("set_config", tenant_sql)
        self.assertIn(", true)", tenant_sql)

    def test_skips_set_config_entirely_when_no_tenant_on_request_state(self):
        """Fail-closed guarantee: a request with no resolved tenant must NOT
        fall back to some default/previous tenant -- it must issue zero
        set_config calls, so RLS denies all rows for that session."""
        session = FakeDBSession()
        request = _fake_request()  # no tenant_id/user_id at all

        with patch("core.database.SessionLocal", return_value=session):
            next(get_db(request))

        self.assertEqual(session.executed, [])

    def test_sets_only_tenant_guc_when_user_id_absent(self):
        session = FakeDBSession()
        request = _fake_request(tenant_id="tenant-only")

        with patch("core.database.SessionLocal", return_value=session):
            next(get_db(request))

        self.assertEqual(len(session.executed), 1)
        tenant_sql, tenant_params = session.executed[0]
        self.assertIn("app.tenant_id", tenant_sql)
        self.assertEqual(tenant_params["tenant_id"], "tenant-only")

    def test_session_closed_even_if_generator_is_not_fully_consumed(self):
        session = FakeDBSession()
        request = _fake_request(tenant_id="tenant-x")

        with patch("core.database.SessionLocal", return_value=session):
            gen = get_db(request)
            next(gen)
            gen.close()  # simulates FastAPI tearing down the dependency

        self.assertTrue(session.closed)


class MultiTenantEndToEndIsolationTests(unittest.TestCase):
    """Simulates two different tenants calling the API back-to-back and
    asserts each request's resolved identity never bleeds into the other."""

    def _build_app(self):
        app = FastAPI()

        @app.get("/whoami")
        async def whoami(request: Request):
            return {
                "tenant_id": getattr(request.state, "tenant_id", None),
                "user_id": getattr(request.state, "user_id", None),
            }

        app.middleware("http")(auth_middleware)
        return app

    def test_sequential_requests_from_different_tenants_stay_isolated(self):
        client = TestClient(self._build_app())

        with patch("core.auth.SSO_LOGIN", False):
            resp_a = client.get(
                "/whoami",
                headers={"X-Tenant-Id": "tenant-A", "X-User-Id": "user-A"},
            )
            resp_b = client.get(
                "/whoami",
                headers={"X-Tenant-Id": "tenant-B", "X-User-Id": "user-B"},
            )
            # A third, unrelated request without headers must NOT inherit
            # tenant B's (or any previous request's) identity.
            resp_c = client.get("/whoami")

        self.assertEqual(resp_a.json(), {"tenant_id": "tenant-A", "user_id": "user-A"})
        self.assertEqual(resp_b.json(), {"tenant_id": "tenant-B", "user_id": "user-B"})
        self.assertEqual(resp_c.json(), {"tenant_id": None, "user_id": None})

    def test_two_authenticated_users_with_different_tenant_claims_stay_isolated(self):
        client = TestClient(self._build_app())
        fake_redis = FakeRedis()
        import json

        fake_redis.set(
            "auth:token:token-for-tenant-1",
            json.dumps({"sub": "u1", "tenant_id": "tenant-1", "user_id": "user-1", "exp": 9999999999}),
        )
        fake_redis.set(
            "auth:token:token-for-tenant-2",
            json.dumps({"sub": "u2", "tenant_id": "tenant-2", "user_id": "user-2", "exp": 9999999999}),
        )

        with patch("core.auth.SSO_LOGIN", True), patch("core.auth.get_redis_client", return_value=fake_redis):
            resp_1 = client.get("/whoami", headers={"Authorization": "Bearer token-for-tenant-1"})
            resp_2 = client.get("/whoami", headers={"Authorization": "Bearer token-for-tenant-2"})
            resp_1_again = client.get("/whoami", headers={"Authorization": "Bearer token-for-tenant-1"})

        self.assertEqual(resp_1.json(), {"tenant_id": "tenant-1", "user_id": "user-1"})
        self.assertEqual(resp_2.json(), {"tenant_id": "tenant-2", "user_id": "user-2"})
        self.assertEqual(resp_1_again.json(), {"tenant_id": "tenant-1", "user_id": "user-1"})


if __name__ == "__main__":
    unittest.main()
