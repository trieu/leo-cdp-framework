"""Keycloak-based authentication middleware for the Customer 360 API.

Also resolves the caller's ``tenant_id`` / ``user_id`` (sys_tenant.tenant_id
/ sys_user.user_id) onto ``request.state`` so ``core.database.get_db`` can
set the ``app.tenant_id`` / ``app.user_id`` Postgres session variables that
the tenant_policy Row-Level Security policies rely on (see the "ROW LEVEL
SECURITY" section of core-customer360/database-schema.sql).
"""

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
from sqlalchemy import text

from core.cache import get_redis_client
from core.config import settings

logger = logging.getLogger(__name__)

EXEMPT_PATHS = {
    "/health",
}
SSO_LOGIN=settings.sso_login
# TTL for the resolved (tenant_id, user_id) identity cache, independent of
# the Keycloak token TTL -- keeps a sys_user lookup off the hot path without
# staying stale for too long if a user's tenant/role changes.
IDENTITY_CACHE_TTL_SECONDS = 300


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


def _load_cached_identity(keycloak_user_id: str) -> Optional[dict[str, str]]:
    client = get_redis_client()
    if client is None:
        return None
    try:
        raw = client.get(f"auth:identity:{keycloak_user_id}")
    except Exception:
        logger.warning("Failed to read cached identity from Redis", exc_info=True)
        return None
    if not raw:
        return None
    try:
        return json.loads(raw)
    except Exception:
        logger.warning("Cached identity payload was not valid JSON", exc_info=True)
        return None


def _cache_identity(keycloak_user_id: str, identity: dict[str, str]) -> None:
    client = get_redis_client()
    if client is None:
        return
    try:
        client.set(f"auth:identity:{keycloak_user_id}", json.dumps(identity), ex=IDENTITY_CACHE_TTL_SECONDS)
    except Exception:
        logger.warning("Failed to cache resolved identity in Redis", exc_info=True)


def _get_or_create_user_on_login(payload: dict[str, Any]) -> Optional[dict[str, str]]:
    """Provisions/updates sys_user on a successful Keycloak login, keyed by
    the token's ``sub`` claim (stored as ``sys_user.keycloak_user_id``).

    - **Existing user**: stamps ``last_login_at = now()`` and returns their
      ``(user_id, tenant_id)``.
    - **First-ever login for this identity**: auto-provisions a new sys_user
      row. This REQUIRES the token to carry a ``tenant_id`` custom claim
      (published via a Keycloak protocol mapper) identifying which tenant
      this identity belongs to -- without it we refuse to provision (fail
      closed: the caller gets no tenant context / no RLS-visible rows,
      rather than being silently assigned to the wrong tenant).

    Local import of SessionLocal avoids a hard import-time dependency
    between core.auth and core.database.
    """
    keycloak_user_id = payload.get("sub")
    if not keycloak_user_id:
        return None

    from core.database import SessionLocal

    db = SessionLocal()
    try:
        row = db.execute(
            text(f"SELECT user_id, tenant_id FROM {settings.db_schema}.sys_user WHERE keycloak_user_id = :kid"),
            {"kid": keycloak_user_id},
        ).mappings().first()

        if row is not None:
            # Existing user: just refresh last_login_at.
            db.execute(
                text(f"UPDATE {settings.db_schema}.sys_user SET last_login_at = now() WHERE user_id = :uid"),
                {"uid": row["user_id"]},
            )
            db.commit()
            return {"user_id": str(row["user_id"]), "tenant_id": str(row["tenant_id"])}

        # New identity: auto-provision, but only if we know which tenant.
        tenant_id = payload.get("tenant_id")
        if not tenant_id:
            logger.warning(
                "Cannot auto-provision sys_user for keycloak_user_id=%s: token has no tenant_id claim",
                keycloak_user_id,
            )
            return None

        username = payload.get("preferred_username") or payload.get("email") or keycloak_user_id
        email = payload.get("email")
        full_name = payload.get("name")

        inserted = db.execute(
            text(
                f"""
                INSERT INTO {settings.db_schema}.sys_user
                    (tenant_id, keycloak_user_id, username, email, full_name, last_login_at)
                VALUES (:tenant_id, :kid, :username, :email, :full_name, now())
                RETURNING user_id, tenant_id
                """
            ),
            {
                "tenant_id": tenant_id,
                "kid": keycloak_user_id,
                "username": username,
                "email": email,
                "full_name": full_name,
            },
        ).mappings().first()
        db.commit()
        if inserted is None:
            return None
        return {"user_id": str(inserted["user_id"]), "tenant_id": str(inserted["tenant_id"])}
    except Exception:
        db.rollback()
        logger.warning("Failed to get-or-create sys_user on Keycloak login", exc_info=True)
        return None
    finally:
        db.close()


def _resolve_tenant_and_user(payload: dict[str, Any]) -> tuple[Optional[str], Optional[str]]:
    """Resolves (tenant_id, user_id) for an authenticated request.

    Prefers explicit ``tenant_id``/``user_id`` custom claims on the Keycloak
    token (if a protocol mapper publishes them); otherwise falls back to a
    (Redis-cached) ``sys_user`` get-or-create keyed by the standard ``sub``
    claim -- see ``_get_or_create_user_on_login``. Caching means last_login_at
    is refreshed at most once per IDENTITY_CACHE_TTL_SECONDS per user, not on
    every single request.
    """
    tenant_id = payload.get("tenant_id")
    user_id = payload.get("user_id")
    if tenant_id and user_id:
        return tenant_id, user_id

    keycloak_user_id = payload.get("sub")
    if not keycloak_user_id:
        return tenant_id, user_id

    identity = _load_cached_identity(keycloak_user_id)
    if identity is None:
        identity = _get_or_create_user_on_login(payload)
        if identity is not None:
            _cache_identity(keycloak_user_id, identity)

    if identity is not None:
        tenant_id = tenant_id or identity.get("tenant_id")
        user_id = user_id or identity.get("user_id")
    return tenant_id, user_id


def _apply_dev_tenant_headers(request: Request) -> None:
    """Dev/test convenience only: when SSO_LOGIN is disabled, trust
    X-Tenant-Id/X-User-Id headers so the app.tenant_id RLS session variable
    (see core/database.py) is still set without a full Keycloak setup.
    NEVER used when SSO_LOGIN=true -- tenant/user identity must then come
    exclusively from the verified token (see _resolve_tenant_and_user)."""
    tenant_id = request.headers.get("X-Tenant-Id")
    user_id = request.headers.get("X-User-Id")
    if tenant_id:
        request.state.tenant_id = tenant_id
    if user_id:
        request.state.user_id = user_id


async def auth_middleware(request: Request, call_next):
    """Ensure API requests present a valid Keycloak bearer token before continuing."""
    if request.method == "OPTIONS":
        return await call_next(request)

    if not SSO_LOGIN or request.url.path in EXEMPT_PATHS:
        _apply_dev_tenant_headers(request)
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

    tenant_id, user_id = _resolve_tenant_and_user(payload)
    if tenant_id:
        request.state.tenant_id = tenant_id
    if user_id:
        request.state.user_id = user_id

    return await call_next(request)
