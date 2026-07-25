import json
import unittest
from unittest.mock import patch

from fastapi import FastAPI, Request
from fastapi.testclient import TestClient

from core.auth import auth_middleware


class FakeRedis:
    def __init__(self):
        self.store = {}

    def get(self, key):
        return self.store.get(key)

    def set(self, key, value, ex=None):
        self.store[key] = value


class AuthMiddlewareTests(unittest.TestCase):
    def test_rejects_requests_without_bearer_token(self):
        app = FastAPI()

        @app.get("/secure")
        async def secure(request: Request):
            return {"ok": True}

        app.middleware("http")(auth_middleware)
        client = TestClient(app)

        response = client.get("/secure")

        self.assertEqual(response.status_code, 401)
        self.assertEqual(response.json()["detail"], "Authentication required")

    def test_allows_cached_valid_token(self):
        app = FastAPI()

        @app.get("/secure")
        async def secure(request: Request):
            return {"ok": True, "sub": request.state.user["sub"]}

        app.middleware("http")(auth_middleware)
        client = TestClient(app)

        fake_redis = FakeRedis()
        fake_redis.set(
            "auth:token:abc123",
            json.dumps({"sub": "user-1", "preferred_username": "demo", "exp": 9999999999}),
        )

        with patch("core.auth.get_redis_client", return_value=fake_redis), patch(
            "core.auth._introspect_with_keycloak", side_effect=AssertionError("should not call Keycloak")
        ):
            response = client.get("/secure", headers={"Authorization": "Bearer abc123"})

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["sub"], "user-1")


if __name__ == "__main__":
    unittest.main()
