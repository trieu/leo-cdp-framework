"""Unit tests for core.auth.auth_middleware.

These tests explicitly patch ``core.auth.SSO_LOGIN`` rather than relying on
the ambient ``.env`` (which has ``SSO_LOGIN=false`` for local dev) so the
suite behaves the same regardless of how/where it's run.
"""

import json
import unittest
from unittest.mock import patch

from fastapi import FastAPI, Request
from fastapi.testclient import TestClient

from core.auth import auth_middleware
from tests.conftest import FakeRedis


def _build_app():
    app = FastAPI()

    @app.get("/secure")
    async def secure(request: Request):
        return {
            "ok": True,
            "sub": request.state.user["sub"] if hasattr(request.state, "user") else None,
            "tenant_id": getattr(request.state, "tenant_id", None),
            "user_id": getattr(request.state, "user_id", None),
        }

    @app.get("/health")
    async def health():
        return {"status": "ok"}

    app.middleware("http")(auth_middleware)
    return app


class AuthMiddlewareTests(unittest.TestCase):
    def test_rejects_requests_without_bearer_token(self):
        client = TestClient(_build_app())

        with patch("core.auth.SSO_LOGIN", True):
            response = client.get("/secure")

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json()["detail"], "Authentication required")

    def test_rejects_malformed_authorization_header(self):
        client = TestClient(_build_app())

        with patch("core.auth.SSO_LOGIN", True):
            response = client.get("/secure", headers={"Authorization": "Basic abc123"})

        self.assertEqual(response.status_code, 401)

    def test_rejects_inactive_or_expired_token(self):
        client = TestClient(_build_app())
        fake_redis = FakeRedis()

        with patch("core.auth.SSO_LOGIN", True), patch(
            "core.auth.get_redis_client", return_value=fake_redis
        ), patch("core.auth._introspect_with_keycloak", return_value={"active": False}):
            response = client.get("/secure", headers={"Authorization": "Bearer expired-token"})

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json()["detail"], "Invalid or expired token")

    def test_rejects_keycloak_unreachable(self):
        """Keycloak introspection returning None (network error, etc.) must
        fail closed (401), never silently let the request through."""
        client = TestClient(_build_app())
        fake_redis = FakeRedis()

        with patch("core.auth.SSO_LOGIN", True), patch(
            "core.auth.get_redis_client", return_value=fake_redis
        ), patch("core.auth._introspect_with_keycloak", return_value=None):
            response = client.get("/secure", headers={"Authorization": "Bearer whatever"})

        self.assertEqual(response.status_code, 401)

    def test_allows_cached_valid_token(self):
        client = TestClient(_build_app())

        fake_redis = FakeRedis()
        fake_redis.set(
            "auth:token:abc123",
            json.dumps({"sub": "user-1", "preferred_username": "demo", "exp": 9999999999}),
        )

        with patch("core.auth.SSO_LOGIN", True), patch(
            "core.auth.get_redis_client", return_value=fake_redis
        ), patch("core.auth._introspect_with_keycloak", side_effect=AssertionError("should not call Keycloak")):
            response = client.get("/secure", headers={"Authorization": "Bearer abc123"})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["sub"], "user-1")

    def test_valid_uncached_token_is_introspected_and_then_cached(self):
        client = TestClient(_build_app())
        fake_redis = FakeRedis()

        introspect_payload = {"active": True, "sub": "user-2", "preferred_username": "new-guy", "exp": 9999999999}

        with patch("core.auth.SSO_LOGIN", True), patch(
            "core.auth.get_redis_client", return_value=fake_redis
        ), patch("core.auth._introspect_with_keycloak", return_value=introspect_payload) as mock_introspect, patch(
            "core.auth._resolve_tenant_and_user", return_value=(None, None)
        ):
            response = client.get("/secure", headers={"Authorization": "Bearer newtoken"})

        self.assertEqual(response.status_code, 200)
        mock_introspect.assert_called_once()
        # Second request with the same token must hit the cache, not Keycloak again.
        with patch("core.auth.SSO_LOGIN", True), patch(
            "core.auth.get_redis_client", return_value=fake_redis
        ), patch(
            "core.auth._introspect_with_keycloak", side_effect=AssertionError("should not call Keycloak again")
        ), patch("core.auth._resolve_tenant_and_user", return_value=(None, None)):
            response2 = client.get("/secure", headers={"Authorization": "Bearer newtoken"})
        self.assertEqual(response2.status_code, 200)

    def test_health_endpoint_is_exempt_even_without_token(self):
        client = TestClient(_build_app())

        with patch("core.auth.SSO_LOGIN", True):
            response = client.get("/health")

        self.assertEqual(response.status_code, 200)

    def test_options_requests_bypass_auth(self):
        client = TestClient(_build_app())

        with patch("core.auth.SSO_LOGIN", True):
            response = client.options("/secure")

        # Starlette/FastAPI's default OPTIONS handling (405/200 depending on
        # route config) is irrelevant here -- the key assertion is that the
        # middleware itself never returns our 401 "Authentication required".
        self.assertNotEqual(response.status_code, 401)

    def test_dev_mode_trusts_tenant_headers_when_sso_disabled(self):
        """When SSO_LOGIN=false (local/dev), X-Tenant-Id/X-User-Id headers
        should populate request.state so app.tenant_id RLS still works."""
        client = TestClient(_build_app())

        with patch("core.auth.SSO_LOGIN", False):
            response = client.get(
                "/secure",
                headers={"X-Tenant-Id": "11111111-1111-1111-1111-111111111111", "X-User-Id": "user-dev-1"},
            )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["tenant_id"], "11111111-1111-1111-1111-111111111111")
        self.assertEqual(body["user_id"], "user-dev-1")

    def test_dev_mode_without_headers_leaves_tenant_unset(self):
        """Fail-closed guarantee: no headers -> no tenant context -> RLS
        will deny all rows rather than defaulting to some tenant."""
        client = TestClient(_build_app())

        with patch("core.auth.SSO_LOGIN", False):
            response = client.get("/secure")

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertIsNone(body["tenant_id"])
        self.assertIsNone(body["user_id"])

    def test_authenticated_request_resolves_tenant_and_user_onto_state(self):
        """End-to-end: a valid token whose payload carries explicit
        tenant_id/user_id claims must populate request.state accordingly."""
        client = TestClient(_build_app())
        fake_redis = FakeRedis()
        fake_redis.set(
            "auth:token:tok-multi",
            json.dumps(
                {
                    "sub": "user-3",
                    "tenant_id": "22222222-2222-2222-2222-222222222222",
                    "user_id": "33333333-3333-3333-3333-333333333333",
                    "exp": 9999999999,
                }
            ),
        )

        with patch("core.auth.SSO_LOGIN", True), patch("core.auth.get_redis_client", return_value=fake_redis):
            response = client.get("/secure", headers={"Authorization": "Bearer tok-multi"})

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["tenant_id"], "22222222-2222-2222-2222-222222222222")
        self.assertEqual(body["user_id"], "33333333-3333-3333-3333-333333333333")


if __name__ == "__main__":
    unittest.main()

