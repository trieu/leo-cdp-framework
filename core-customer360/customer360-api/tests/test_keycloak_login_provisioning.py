"""Unit tests for Keycloak login -> sys_user provisioning
(core.auth._get_or_create_user_on_login / _resolve_tenant_and_user).

Covers the "first login creates a sys_user row, subsequent logins just
refresh last_login_at" behavior, the fail-closed rule when a brand-new
identity's token carries no tenant_id claim, and the Redis identity cache.
"""

import unittest
from unittest.mock import patch

from core.auth import _get_or_create_user_on_login, _resolve_tenant_and_user
from tests.conftest import FakeDBSession, FakeQueryResult


class GetOrCreateUserOnLoginTests(unittest.TestCase):
    def test_new_user_is_created_when_tenant_claim_present(self):
        payload = {
            "sub": "kc-new-user-1",
            "tenant_id": "11111111-1111-1111-1111-111111111111",
            "preferred_username": "alice",
            "email": "alice@example.com",
            "name": "Alice Anderson",
        }
        session = FakeDBSession(
            script=[
                FakeQueryResult(None),  # SELECT ... WHERE keycloak_user_id -> not found
                FakeQueryResult({"user_id": "new-user-1", "tenant_id": "11111111-1111-1111-1111-111111111111"}),
            ]
        )

        with patch("core.database.SessionLocal", return_value=session):
            result = _get_or_create_user_on_login(payload)

        self.assertEqual(
            result, {"user_id": "new-user-1", "tenant_id": "11111111-1111-1111-1111-111111111111"}
        )
        self.assertTrue(session.committed)
        self.assertFalse(session.rolled_back)
        # First call = SELECT lookup, second call = INSERT ... RETURNING.
        self.assertEqual(len(session.executed), 2)
        select_sql, select_params = session.executed[0]
        insert_sql, insert_params = session.executed[1]
        self.assertIn("SELECT", select_sql)
        self.assertIn("INSERT INTO", insert_sql)
        self.assertEqual(insert_params["tenant_id"], "11111111-1111-1111-1111-111111111111")
        self.assertEqual(insert_params["kid"], "kc-new-user-1")
        self.assertEqual(insert_params["username"], "alice")
        self.assertEqual(insert_params["email"], "alice@example.com")
        self.assertEqual(insert_params["full_name"], "Alice Anderson")

    def test_new_user_without_tenant_claim_is_refused_fail_closed(self):
        payload = {"sub": "kc-new-user-2"}  # no tenant_id claim at all
        session = FakeDBSession(script=[FakeQueryResult(None)])  # SELECT -> not found

        with patch("core.database.SessionLocal", return_value=session):
            result = _get_or_create_user_on_login(payload)

        self.assertIsNone(result)
        # Only the SELECT should have run -- no INSERT without a known tenant.
        self.assertEqual(len(session.executed), 1)
        self.assertFalse(session.committed)

    def test_existing_user_last_login_is_refreshed_not_recreated(self):
        payload = {"sub": "kc-existing-1"}
        session = FakeDBSession(
            script=[
                FakeQueryResult({"user_id": "existing-user-1", "tenant_id": "22222222-2222-2222-2222-222222222222"})
            ]
        )

        with patch("core.database.SessionLocal", return_value=session):
            result = _get_or_create_user_on_login(payload)

        self.assertEqual(
            result, {"user_id": "existing-user-1", "tenant_id": "22222222-2222-2222-2222-222222222222"}
        )
        self.assertTrue(session.committed)
        self.assertEqual(len(session.executed), 2)
        update_sql, update_params = session.executed[1]
        self.assertIn("UPDATE", update_sql)
        self.assertIn("last_login_at", update_sql)
        self.assertEqual(update_params["uid"], "existing-user-1")
        # Must never attempt to (re-)insert an existing user.
        self.assertNotIn("INSERT INTO", update_sql)

    def test_missing_sub_claim_returns_none_without_touching_db(self):
        with patch("core.database.SessionLocal") as mock_session_local:
            result = _get_or_create_user_on_login({"preferred_username": "no-sub-claim"})

        self.assertIsNone(result)
        mock_session_local.assert_not_called()

    def test_database_error_rolls_back_and_returns_none(self):
        payload = {"sub": "kc-broken"}
        session = FakeDBSession(raise_on_call=1)  # SELECT itself raises

        with patch("core.database.SessionLocal", return_value=session):
            result = _get_or_create_user_on_login(payload)

        self.assertIsNone(result)
        self.assertTrue(session.rolled_back)
        self.assertTrue(session.closed)

    def test_session_is_always_closed(self):
        payload = {"sub": "kc-existing-2"}
        session = FakeDBSession(
            script=[FakeQueryResult({"user_id": "u", "tenant_id": "t"})]
        )

        with patch("core.database.SessionLocal", return_value=session):
            _get_or_create_user_on_login(payload)

        self.assertTrue(session.closed)


class ResolveTenantAndUserTests(unittest.TestCase):
    def test_explicit_token_claims_short_circuit_db_and_cache(self):
        payload = {
            "sub": "kc-x",
            "tenant_id": "tenant-from-claim",
            "user_id": "user-from-claim",
        }

        with patch("core.auth._load_cached_identity", side_effect=AssertionError("should not check cache")), patch(
            "core.auth._get_or_create_user_on_login", side_effect=AssertionError("should not hit DB")
        ):
            tenant_id, user_id = _resolve_tenant_and_user(payload)

        self.assertEqual((tenant_id, user_id), ("tenant-from-claim", "user-from-claim"))

    def test_missing_sub_and_claims_returns_none_none(self):
        tenant_id, user_id = _resolve_tenant_and_user({"preferred_username": "no-identity"})
        self.assertIsNone(tenant_id)
        self.assertIsNone(user_id)

    def test_cache_hit_avoids_db_lookup(self):
        payload = {"sub": "kc-cached-1"}

        with patch(
            "core.auth._load_cached_identity",
            return_value={"tenant_id": "tenant-cached", "user_id": "user-cached"},
        ), patch("core.auth._get_or_create_user_on_login", side_effect=AssertionError("must not hit DB on cache hit")):
            tenant_id, user_id = _resolve_tenant_and_user(payload)

        self.assertEqual((tenant_id, user_id), ("tenant-cached", "user-cached"))

    def test_cache_miss_resolves_via_db_and_populates_cache(self):
        payload = {"sub": "kc-cache-miss-1"}

        with patch("core.auth._load_cached_identity", return_value=None), patch(
            "core.auth._get_or_create_user_on_login",
            return_value={"user_id": "u-1", "tenant_id": "t-1"},
        ), patch("core.auth._cache_identity") as mock_cache_identity:
            tenant_id, user_id = _resolve_tenant_and_user(payload)

        self.assertEqual((tenant_id, user_id), ("t-1", "u-1"))
        mock_cache_identity.assert_called_once_with("kc-cache-miss-1", {"user_id": "u-1", "tenant_id": "t-1"})

    def test_failed_provisioning_returns_none_none_and_does_not_cache(self):
        payload = {"sub": "kc-cannot-provision"}

        with patch("core.auth._load_cached_identity", return_value=None), patch(
            "core.auth._get_or_create_user_on_login", return_value=None
        ), patch("core.auth._cache_identity") as mock_cache_identity:
            tenant_id, user_id = _resolve_tenant_and_user(payload)

        self.assertIsNone(tenant_id)
        self.assertIsNone(user_id)
        mock_cache_identity.assert_not_called()


class MultiTenantLoginIsolationTests(unittest.TestCase):
    """Two different Keycloak identities logging in must never bleed into
    each other's tenant/user resolution."""

    def test_two_different_users_resolve_to_their_own_distinct_tenants(self):
        sessions = {
            "kc-tenant-a-user": FakeDBSession(
                script=[FakeQueryResult({"user_id": "user-a", "tenant_id": "tenant-a"})]
            ),
            "kc-tenant-b-user": FakeDBSession(
                script=[FakeQueryResult({"user_id": "user-b", "tenant_id": "tenant-b"})]
            ),
        }

        results = {}
        for sub, session in sessions.items():
            with patch("core.database.SessionLocal", return_value=session):
                results[sub] = _get_or_create_user_on_login({"sub": sub})

        self.assertEqual(results["kc-tenant-a-user"], {"user_id": "user-a", "tenant_id": "tenant-a"})
        self.assertEqual(results["kc-tenant-b-user"], {"user_id": "user-b", "tenant_id": "tenant-b"})
        self.assertNotEqual(results["kc-tenant-a-user"]["tenant_id"], results["kc-tenant-b-user"]["tenant_id"])


if __name__ == "__main__":
    unittest.main()
