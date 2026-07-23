"""Unit tests for identity_resolution.resolver.CustomerIdentityResolver.

No real database is used: psycopg2 connection/cursor objects are mocked via
the `mock_conn` / `mock_cursor` fixtures in conftest.py.
"""

import hashlib

from identity_resolution.models import IdentityRule
from identity_resolution.persona import generate_persona_name
from identity_resolution.resolver import CustomerIdentityResolver


def make_resolver(mock_conn, **kwargs):
    return CustomerIdentityResolver(mock_conn, schema="customer360", **kwargs)


class TestGetActiveRules:
    def test_returns_identity_rules_from_metadata(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.return_value = [
            {"attribute_internal_code": "email", "matching_rule": "exact", "matching_threshold": None},
            {
                "attribute_internal_code": "full_name",
                "matching_rule": "fuzzy_trgm",
                "matching_threshold": 0.7,
            },
        ]
        resolver = make_resolver(mock_conn)

        rules = resolver._get_active_rules(mock_cursor)

        assert rules == [
            IdentityRule("email", "exact", None),
            IdentityRule("full_name", "fuzzy_trgm", 0.7),
        ]
        query = mock_cursor.execute.call_args[0][0]
        assert "is_identity_resolution = TRUE" in query
        assert "cdp_profile_attributes" in query

    def test_returns_empty_list_when_no_active_rules(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.return_value = []
        resolver = make_resolver(mock_conn)

        assert resolver._get_active_rules(mock_cursor) == []


class TestFetchUnprocessedProfiles:
    def test_uses_batch_size_and_status_code_filter(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.return_value = [{"raw_profile_id": "r1"}]
        resolver = make_resolver(mock_conn, batch_size=42)

        result = resolver._fetch_unprocessed_profiles(mock_cursor)

        assert result == [{"raw_profile_id": "r1"}]
        query, params = mock_cursor.execute.call_args[0]
        assert "status_code = 1" in query
        assert params == (42,)


class TestFindMasterProfile:
    def test_exact_scalar_match_found(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "master-1"}
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r1",
            "tenant_id": "t1",
            "domain": "banking",
            "email": "john@example.com",
        }
        rules = [IdentityRule("email", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result == "master-1"
        query, params = mock_cursor.execute.call_args[0]
        assert "tenant_id = %s AND domain = %s" in query
        assert "email = %s" in query
        assert params == ("t1", "banking", "john@example.com")

    def test_no_match_returns_none(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = None
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "tenant_id": "t1", "email": "unknown@example.com"}
        rules = [IdentityRule("email", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result is None

    def test_defaults_domain_to_retail_when_missing(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = None
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "tenant_id": "t1", "email": "a@b.com"}
        rules = [IdentityRule("email", "exact")]

        resolver._find_master_profile(mock_cursor, raw_profile, rules)

        _, params = mock_cursor.execute.call_args[0]
        assert params == ("t1", "retail", "a@b.com")

    def test_skips_rules_when_raw_value_missing(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "tenant_id": "t1", "email": None}
        rules = [IdentityRule("email", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result is None
        mock_cursor.execute.assert_not_called()

    def test_array_identity_field_device_id(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "master-2"}
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "tenant_id": "t1", "domain": "retail", "device_id": "dev-123"}
        rules = [IdentityRule("device_id", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result == "master-2"
        query, params = mock_cursor.execute.call_args[0]
        assert "%s = ANY(device_ids)" in query
        assert params == ("t1", "retail", "dev-123")

    def test_jsonb_keyed_external_customer_id_requires_source_system(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "master-3"}
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r1",
            "tenant_id": "t1",
            "domain": "retail",
            "source_system": "MoEngage",
            "external_customer_id": "moe-999",
        }
        rules = [IdentityRule("external_customer_id", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result == "master-3"
        query, params = mock_cursor.execute.call_args[0]
        assert "external_ids @> jsonb_build_object(%s::text, %s::text)" in query
        assert params == ("t1", "retail", "MoEngage", "moe-999")

    def test_jsonb_keyed_field_skipped_without_source_system(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r1",
            "tenant_id": "t1",
            "external_customer_id": "moe-999",
        }
        rules = [IdentityRule("external_customer_id", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result is None
        mock_cursor.execute.assert_not_called()

    def test_fuzzy_trgm_uses_threshold(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "master-4"}
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "tenant_id": "t1", "full_name": "Nguyen Van A"}
        rules = [IdentityRule("full_name", "fuzzy_trgm", threshold=0.65)]

        resolver._find_master_profile(mock_cursor, raw_profile, rules)

        query, params = mock_cursor.execute.call_args[0]
        assert "similarity(full_name, %s) >= %s" in query
        assert params == ("t1", "retail", "Nguyen Van A", 0.65)

    def test_fuzzy_trgm_defaults_threshold_when_missing(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = None
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "tenant_id": "t1", "full_name": "Nguyen Van A"}
        rules = [IdentityRule("full_name", "fuzzy_trgm", threshold=None)]

        resolver._find_master_profile(mock_cursor, raw_profile, rules)

        _, params = mock_cursor.execute.call_args[0]
        assert params == ("t1", "retail", "Nguyen Van A", 0.7)

    def test_fuzzy_dmetaphone_rule(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "master-5"}
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "tenant_id": "t1", "full_name": "Jon Smyth"}
        rules = [IdentityRule("full_name", "fuzzy_dmetaphone")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result == "master-5"
        query, params = mock_cursor.execute.call_args[0]
        assert "dmetaphone(full_name) = dmetaphone(%s)" in query
        assert params == ("t1", "retail", "Jon Smyth")

    def test_unknown_rule_is_skipped(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "tenant_id": "t1", "email": "a@b.com"}
        rules = [IdentityRule("email", "some_unknown_rule")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result is None
        mock_cursor.execute.assert_not_called()


class TestLinkAndUpdate:
    def test_inserts_link_and_updates_master(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r1",
            "tenant_id": "t1",
            "domain": "banking",
            "full_name": "Nguyen Van A",
            "email": "a@example.com",
            "phone_number": None,
            "national_id": "079123456789",
            "device_id": "dev-1",
            "advertising_id": "adv-1",
            "cookie_id": None,
            "external_customer_id": "af-1",
            "push_token": "push-token-1",
            "source_system": "AppsFlyer",
        }

        resolver._link_and_update(mock_cursor, raw_profile, "master-1")

        assert mock_cursor.execute.call_count == 2
        link_query, link_params = mock_cursor.execute.call_args_list[0][0]
        assert "INSERT INTO customer360.cdp_profile_links" in link_query
        assert "ON CONFLICT (tenant_id, raw_profile_id) DO NOTHING" in link_query
        assert link_params == ("t1", "r1", "master-1", 1.0, "DynamicMatch")

        update_query, update_params = mock_cursor.execute.call_args_list[1][0]
        assert "UPDATE customer360.cdp_master_profiles" in update_query
        assert "COALESCE(full_name, %s)" in update_query
        assert "device_ids = CASE WHEN %s = ANY(COALESCE(device_ids, ARRAY[]::TEXT[]))" in update_query
        assert "external_ids = COALESCE(external_ids, '{}'::JSONB)" in update_query
        assert "push_tokens = COALESCE(push_tokens, '{}'::JSONB)" in update_query
        assert "source_systems = CASE WHEN %s = ANY" in update_query
        assert "is_hashed = is_hashed OR %s" in update_query
        assert "persona_name = COALESCE(persona_name, %s)" in update_query
        assert update_query.strip().endswith(
            "WHERE master_profile_id = %s AND tenant_id = %s;"
        )
        # Plaintext full_name here (not a SHA-256 hex digest) -> not flagged hashed, no persona_name.
        assert update_params[-4:] == (False, None, "master-1", "t1")

    def test_skips_identity_graph_merges_without_source_system(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r1",
            "tenant_id": "t1",
            "full_name": "Nguyen Van A",
            "email": None,
            "phone_number": None,
            "national_id": None,
        }

        resolver._link_and_update(mock_cursor, raw_profile, "master-1")

        update_query, _ = mock_cursor.execute.call_args_list[1][0]
        assert "external_ids" not in update_query
        assert "push_tokens" not in update_query
        assert "source_systems" not in update_query

    def test_sets_is_hashed_and_persona_name_when_pii_looks_hashed(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        hashed_full_name = hashlib.sha256(b"nguyen van a").hexdigest()
        raw_profile = {
            "raw_profile_id": "r1",
            "tenant_id": "t1",
            "domain": "retail",
            "full_name": hashed_full_name,
            "email": None,
            "phone_number": None,
            "national_id": None,
            "device_id": "dev-1",
            "media_source": "TikTok Ads",
        }

        resolver._link_and_update(mock_cursor, raw_profile, "master-1")

        _, update_params = mock_cursor.execute.call_args_list[1][0]
        expected_persona_name = generate_persona_name(raw_profile)
        assert update_params[-4:] == (True, expected_persona_name, "master-1", "t1")


class TestCreateMasterAndLink:
    def test_creates_master_and_links(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "new-master-1"}
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r2",
            "tenant_id": "t1",
            "domain": "retail",
            "full_name": "Tran Thi B",
            "email": "b@example.com",
            "phone_number": "5551234567",
            "national_id": None,
            "device_id": "dev-2",
            "advertising_id": "adv-2",
            "cookie_id": "cookie-2",
            "external_customer_id": "moe-2",
            "push_token": "push-2",
            "source_system": "MoEngage",
        }

        new_id = resolver._create_master_and_link(mock_cursor, raw_profile)

        assert new_id == "new-master-1"
        assert mock_cursor.execute.call_count == 2

        insert_query, insert_params = mock_cursor.execute.call_args_list[0][0]
        assert "INSERT INTO customer360.cdp_master_profiles" in insert_query
        assert "RETURNING master_profile_id" in insert_query
        assert insert_params[0] == "t1"
        assert insert_params[1] == "retail"
        assert insert_params[6].adapted == {"MoEngage": "moe-2"}  # external_ids
        assert insert_params[7] == ["dev-2"]  # device_ids
        assert insert_params[8] == ["adv-2"]  # advertising_ids
        assert insert_params[9] == ["cookie-2"]  # cookie_ids
        assert insert_params[10].adapted == {"MoEngage": "push-2"}  # push_tokens
        assert insert_params[11] == ["MoEngage"]  # source_systems
        assert insert_params[12] == "r2"  # first_seen_raw_profile_id
        # Plaintext full_name here (not a SHA-256 hex digest) -> not flagged hashed, no persona_name.
        assert insert_params[13] is False  # is_hashed
        assert insert_params[14] is None  # persona_name

        link_query, link_params = mock_cursor.execute.call_args_list[1][0]
        assert "INSERT INTO customer360.cdp_profile_links" in link_query
        assert link_params == ("t1", "r2", "new-master-1", 1.0, "NewMaster")

    def test_sets_is_hashed_and_persona_name_when_pii_looks_hashed(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "new-master-2"}
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r3",
            "tenant_id": "t1",
            "domain": "banking",
            "full_name": hashlib.sha256(b"tran thi b").hexdigest(),
            "email": hashlib.sha256(b"b@example.com").hexdigest(),
            "phone_number": None,
            "national_id": None,
            "device_id": "dev-3",
            "media_source": "Facebook Ads",
            "source_system": "AppsFlyer",
        }

        resolver._create_master_and_link(mock_cursor, raw_profile)

        _, insert_params = mock_cursor.execute.call_args_list[0][0]
        assert insert_params[13] is True  # is_hashed
        assert insert_params[14] == generate_persona_name(raw_profile)  # persona_name


class TestMarkAsProcessed:
    def test_sets_status_code_and_processed_at(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)

        resolver._mark_as_processed(mock_cursor, "r1")

        query, params = mock_cursor.execute.call_args[0]
        assert "SET status_code = 3, processed_at = NOW()" in query
        assert params == ("r1",)


class TestRunResolutionBatch:
    def test_no_active_rules_returns_zero(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.return_value = []
        resolver = make_resolver(mock_conn)

        result = resolver.run_resolution_batch()

        assert result == 0
        mock_conn.commit.assert_not_called()

    def test_no_unprocessed_profiles_returns_zero(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.side_effect = [
            [{"attribute_internal_code": "email", "matching_rule": "exact", "matching_threshold": None}],
            [],
        ]
        resolver = make_resolver(mock_conn)

        result = resolver.run_resolution_batch()

        assert result == 0
        mock_conn.commit.assert_not_called()

    def test_processes_matched_and_unmatched_profiles(self, mock_cursor, mock_conn):
        rules = [{"attribute_internal_code": "email", "matching_rule": "exact", "matching_threshold": None}]
        profiles = [
            {
                "raw_profile_id": "r1",
                "tenant_id": "t1",
                "domain": "retail",
                "source_system": "WebTracking",
                "full_name": "Nguyen Van A",
                "email": "a@example.com",
                "phone_number": None,
                "national_id": None,
                "device_id": None,
                "advertising_id": None,
                "cookie_id": "cookie-1",
                "external_customer_id": None,
                "push_token": None,
            },
            {
                "raw_profile_id": "r2",
                "tenant_id": "t1",
                "domain": "retail",
                "source_system": "AppsFlyer",
                "full_name": "Tran Thi B",
                "email": "b@example.com",
                "phone_number": "5551234567",
                "national_id": None,
                "device_id": "dev-2",
                "advertising_id": "adv-2",
                "cookie_id": None,
                "external_customer_id": None,
                "push_token": None,
            },
        ]
        mock_cursor.fetchall.side_effect = [rules, profiles]
        # fetchone is called by: _find_master_profile(r1) -> match,
        # _find_master_profile(r2) -> no match, _create_master_and_link(r2) -> new id
        mock_cursor.fetchone.side_effect = [
            {"master_profile_id": "existing-master"},
            None,
            {"master_profile_id": "new-master"},
        ]
        resolver = make_resolver(mock_conn, batch_size=10)

        processed = resolver.run_resolution_batch()

        assert processed == 2
        mock_conn.commit.assert_called_once()

    def test_rolls_back_and_reraises_on_error(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.side_effect = RuntimeError("boom")
        resolver = make_resolver(mock_conn)

        try:
            resolver.run_resolution_batch()
            assert False, "expected RuntimeError to propagate"
        except RuntimeError:
            pass

        mock_conn.rollback.assert_called_once()
        mock_conn.commit.assert_not_called()

