"""Keycloak-based authentication middleware for the Customer 360 API."""

import json
import logging
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Optional

from fastapi import Request
from fastapi.responses import JSONResponse

from core.cache import get_redis_client
from core.config import settings

logger = logging.getLogger(__name__)

EXEMPT_PATHS = {
    "/health",
}


def _build_introspection_url() -> str:
    base_url = settings.sso_login_url.rstrip("/")
    return (
        f"{base_url}/realms/{settings.keycloak_realm}/protocol/openid-connect/token/introspect"
    )


def _introspect_with_keycloak(token: str) -> Optional[dict[str, Any]]:
    """Validate a bearer token against Keycloak and return the introspection payload."""
    url = _build_introspection_url()
    body = urllib.parse.urlencode(
        {
            "token": token,
            "client_id": settings.keycloak_client_id,
            "client_secret": settings.keycloak_client_secret,
            "token_type_hint": "access_token",
        }
    ).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
        method="POST",
    )

    context = None
    if not settings.keycloak_verify_ssl:
        context = ssl._create_unverified_context()

    try:
        with urllib.request.urlopen(req, timeout=5, context=context) as response:
            payload = json.load(response)
            return payload if isinstance(payload, dict) else None
    except urllib.error.HTTPError as exc:
        logger.warning("Keycloak introspection failed with HTTP %s", exc.code)
        return None
    except Exception:
        logger.warning("Keycloak introspection request failed", exc_info=True)
        return None


def _cache_token(token: str, payload: dict[str, Any]) -> None:
    client = get_redis_client()
    if client is None:
        return

    exp = payload.get("exp")
    ttl_seconds: Optional[int] = None
    if isinstance(exp, (int, float)):
        ttl_seconds = max(60, int(exp) - int(time.time()))
    if ttl_seconds is not None and ttl_seconds > 0:
        try:
            client.set(
                f"auth:token:{token}",
                json.dumps(payload, default=str),
                ex=ttl_seconds,
            )
        except Exception:
            logger.warning("Failed to cache Keycloak token in Redis", exc_info=True)


def _load_cached_token(token: str) -> Optional[dict[str, Any]]:
    client = get_redis_client()
    if client is None:
        return None

    try:
        raw = client.get(f"auth:token:{token}")
    except Exception:
        logger.warning("Failed to read cached token from Redis", exc_info=True)
        return None

    if not raw:
        return None

    try:
        return json.loads(raw)
    except Exception:
        logger.warning("Cached token payload was not valid JSON", exc_info=True)
        return None


async def auth_middleware(request: Request, call_next):
    """Ensure API requests present a valid Keycloak bearer token before continuing."""
    if request.method == "OPTIONS":
        return await call_next(request)

    if request.url.path in EXEMPT_PATHS:
        return await call_next(request)

    authorization = request.headers.get("Authorization", "")
    if not authorization.startswith("Bearer "):
        return JSONResponse(status_code=401, content={"detail": "Authentication required"})

    token = authorization[len("Bearer "):].strip()
    if not token:
        return JSONResponse(status_code=401, content={"detail": "Authentication required"})

    payload = _load_cached_token(token)
    if payload is None:
        payload = _introspect_with_keycloak(token)
        if not payload or not payload.get("active"):
            return JSONResponse(status_code=401, content={"detail": "Invalid or expired token"})
        _cache_token(token, payload)

    request.state.user = payload
    request.state.token = token
    return await call_next(request)
