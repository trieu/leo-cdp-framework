"""Unit tests for identity_resolution.resolver.CustomerIdentityResolver.

No real database is used: psycopg2 connection/cursor objects are mocked via
the `mock_conn` / `mock_cursor` fixtures in conftest.py.
"""

from identity_resolution.models import IdentityRule
from identity_resolution.resolver import CustomerIdentityResolver


def make_resolver(mock_conn, **kwargs):
    return CustomerIdentityResolver(mock_conn, schema="public", **kwargs)


class TestGetActiveRules:
    def test_returns_identity_rules_from_metadata(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.return_value = [
            {"attribute_internal_code": "email", "matching_rule": "exact", "matching_threshold": None},
            {
                "attribute_internal_code": "last_name",
                "matching_rule": "fuzzy_trgm",
                "matching_threshold": 0.7,
            },
        ]
        resolver = make_resolver(mock_conn)

        rules = resolver._get_active_rules(mock_cursor)

        assert rules == [
            IdentityRule("email", "exact", None),
            IdentityRule("last_name", "fuzzy_trgm", 0.7),
        ]
        query = mock_cursor.execute.call_args[0][0]
        assert "is_identity_resolution = TRUE" in query
        assert "cdp_profile_attributes" in query

    def test_returns_empty_list_when_no_active_rules(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.return_value = []
        resolver = make_resolver(mock_conn)

        assert resolver._get_active_rules(mock_cursor) == []


class TestFetchUnprocessedProfiles:
    def test_uses_batch_size_and_processed_at_filter(self, mock_cursor, mock_conn):
        mock_cursor.fetchall.return_value = [{"raw_profile_id": "r1"}]
        resolver = make_resolver(mock_conn, batch_size=42)

        result = resolver._fetch_unprocessed_profiles(mock_cursor)

        assert result == [{"raw_profile_id": "r1"}]
        query, params = mock_cursor.execute.call_args[0]
        assert "processed_at IS NULL" in query
        assert params == (42,)


class TestFindMasterProfile:
    def test_exact_match_found(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "master-1"}
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "email": "john@example.com"}
        rules = [IdentityRule("email", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result == "master-1"
        query, params = mock_cursor.execute.call_args[0]
        assert "email = %s" in query
        assert params == ("john@example.com",)

    def test_no_match_returns_none(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = None
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "email": "unknown@example.com"}
        rules = [IdentityRule("email", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result is None

    def test_skips_rules_when_raw_value_missing(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "email": None}
        rules = [IdentityRule("email", "exact")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result is None
        mock_cursor.execute.assert_not_called()

    def test_fuzzy_trgm_uses_threshold(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "master-2"}
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "last_name": "Smyth"}
        rules = [IdentityRule("last_name", "fuzzy_trgm", threshold=0.65)]

        resolver._find_master_profile(mock_cursor, raw_profile, rules)

        query, params = mock_cursor.execute.call_args[0]
        assert "similarity(last_name, %s) >= %s" in query
        assert params == ("Smyth", 0.65)

    def test_fuzzy_trgm_defaults_threshold_when_missing(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = None
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "last_name": "Smyth"}
        rules = [IdentityRule("last_name", "fuzzy_trgm", threshold=None)]

        resolver._find_master_profile(mock_cursor, raw_profile, rules)

        _, params = mock_cursor.execute.call_args[0]
        assert params == ("Smyth", 0.7)

    def test_fuzzy_dmetaphone_rule(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "master-3"}
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "first_name": "Jon"}
        rules = [IdentityRule("first_name", "fuzzy_dmetaphone")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result == "master-3"
        query, params = mock_cursor.execute.call_args[0]
        assert "dmetaphone(first_name) = dmetaphone(%s)" in query
        assert params == ("Jon",)

    def test_unknown_rule_is_skipped(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        raw_profile = {"raw_profile_id": "r1", "email": "a@b.com"}
        rules = [IdentityRule("email", "some_unknown_rule")]

        result = resolver._find_master_profile(mock_cursor, raw_profile, rules)

        assert result is None
        mock_cursor.execute.assert_not_called()


class TestLinkAndUpdate:
    def test_inserts_link_and_updates_master(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r1",
            "first_name": "Jon",
            "last_name": "Smyth",
            "email": "john@example.com",
            "phone_number": None,
            "address_line1": "123 Elm Street",
            "city": "New York",
            "state": "NY",
            "zip_code": "10001",
            "source_system": "SystemB",
        }

        resolver._link_and_update(mock_cursor, raw_profile, "master-1")

        assert mock_cursor.execute.call_count == 2
        link_query, link_params = mock_cursor.execute.call_args_list[0][0]
        assert "INSERT INTO public.cdp_profile_links" in link_query
        assert "ON CONFLICT (raw_profile_id) DO NOTHING" in link_query
        assert link_params == ("r1", "master-1", "DynamicMatch")

        update_query, update_params = mock_cursor.execute.call_args_list[1][0]
        assert "UPDATE public.cdp_master_profiles" in update_query
        assert "COALESCE(first_name, %s)" in update_query
        assert update_params[-2] == "master-1"


class TestCreateMasterAndLink:
    def test_creates_master_and_links(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"master_profile_id": "new-master-1"}
        resolver = make_resolver(mock_conn)
        raw_profile = {
            "raw_profile_id": "r2",
            "first_name": "Jane",
            "last_name": "Doe",
            "email": "jane.d@example.com",
            "phone_number": "5551234567",
            "address_line1": "456 Oak Ave",
            "city": "Los Angeles",
            "state": "CA",
            "zip_code": "90001",
            "source_system": "SystemA",
        }

        new_id = resolver._create_master_and_link(mock_cursor, raw_profile)

        assert new_id == "new-master-1"
        assert mock_cursor.execute.call_count == 2
        insert_query, _ = mock_cursor.execute.call_args_list[0][0]
        assert "INSERT INTO public.cdp_master_profiles" in insert_query
        assert "RETURNING master_profile_id" in insert_query

        link_query, link_params = mock_cursor.execute.call_args_list[1][0]
        assert "INSERT INTO public.cdp_profile_links" in link_query
        assert link_params == ("r2", "new-master-1", "NewMaster")


class TestMarkAsProcessed:
    def test_sets_processed_at(self, mock_cursor, mock_conn):
        resolver = make_resolver(mock_conn)

        resolver._mark_as_processed(mock_cursor, "r1")

        query, params = mock_cursor.execute.call_args[0]
        assert "SET processed_at = NOW()" in query
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
                "first_name": "Jon",
                "last_name": "Smyth",
                "email": "john@example.com",
                "phone_number": None,
                "address_line1": "123 Elm Street",
                "city": "New York",
                "state": "NY",
                "zip_code": "10001",
                "source_system": "SystemB",
            },
            {
                "raw_profile_id": "r2",
                "first_name": "Jane",
                "last_name": "Doe",
                "email": "jane.d@example.com",
                "phone_number": "5551234567",
                "address_line1": "456 Oak Ave",
                "city": "Los Angeles",
                "state": "CA",
                "zip_code": "90001",
                "source_system": "SystemA",
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
