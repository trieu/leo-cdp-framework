"""Unit tests for core.init_core_data (default segmentation-tag seeding run
at application startup) -- exercised entirely against FakeDBSession, no real
PostgreSQL instance required. See tests/conftest.py.
"""

import unittest
import uuid
from typing import Any
from unittest.mock import patch

from sqlalchemy.exc import IntegrityError

from core.init_core_data import DEFAULT_SEGMENTS, _final_generated_sql, init_core_data, seed_default_segments
from core.models.segmentation import CdpSegment
from tests.conftest import FakeDBSession


class _AllResult:
    """Stands in for a SQLAlchemy CursorResult supporting `.all()`."""

    def __init__(self, rows: list[Any]):
        self._rows = rows

    def all(self):
        return self._rows


class _ScalarResult:
    """Stands in for a SQLAlchemy CursorResult supporting `.scalar_one()`."""

    def __init__(self, value: Any):
        self._value = value

    def scalar_one(self):
        return self._value


class SeedDefaultSegmentsTests(unittest.TestCase):
    def test_no_tenants_inserts_nothing(self):
        session = FakeDBSession(script=[_AllResult([])])

        inserted = seed_default_segments(session)

        self.assertEqual(inserted, 0)
        self.assertEqual(session.added, [])
        self.assertFalse(session.committed)
        # Only the initial "SELECT tenant_id FROM sys_tenant" query was issued.
        self.assertEqual(len(session.executed), 1)
        self.assertIn("sys_tenant", session.executed[0][0])

    def test_seeds_all_default_segments_for_a_tenant_with_none_yet(self):
        tenant_id = uuid.uuid4()
        session = FakeDBSession(
            script=[
                _AllResult([(tenant_id,)]),  # SELECT tenant_id FROM sys_tenant
                None,  # SELECT set_config('app.tenant_id', ...)
                _ScalarResult(0),  # existing cdp_segments count for tenant
            ]
        )

        inserted = seed_default_segments(session)

        self.assertEqual(inserted, len(DEFAULT_SEGMENTS))
        self.assertEqual(len(session.added), len(DEFAULT_SEGMENTS))
        self.assertTrue(all(isinstance(obj, CdpSegment) for obj in session.added))
        self.assertEqual({obj.tenant_id for obj in session.added}, {tenant_id})
        self.assertEqual(
            {obj.segment_tag for obj in session.added},
            {seg["segment_tag"] for seg in DEFAULT_SEGMENTS},
        )
        self.assertTrue(all(obj.processed_by == "human" for obj in session.added))
        self.assertTrue(session.committed)

    def test_sets_tenant_guc_before_touching_cdp_segments(self):
        """Every tenant must be scoped via set_config('app.tenant_id', ...)
        before the cdp_segments count/insert queries run, so RLS never fails
        closed (or leaks a previous tenant's rows) mid-seed."""
        tenant_id = uuid.uuid4()
        session = FakeDBSession(
            script=[
                _AllResult([(tenant_id,)]),
                None,
                _ScalarResult(0),
            ]
        )

        seed_default_segments(session)

        guc_sql, guc_params = session.executed[1]
        self.assertIn("app.tenant_id", guc_sql)
        self.assertEqual(guc_params["tenant_id"], str(tenant_id))

    def test_skips_tenant_that_already_has_segments(self):
        tenant_id = uuid.uuid4()
        session = FakeDBSession(
            script=[
                _AllResult([(tenant_id,)]),
                None,
                _ScalarResult(3),  # tenant already has 3 segments
            ]
        )

        inserted = seed_default_segments(session)

        self.assertEqual(inserted, 0)
        self.assertEqual(session.added, [])
        self.assertFalse(session.committed)

    def test_multiple_tenants_only_seeds_the_ones_without_segments(self):
        seeded_tenant = uuid.uuid4()
        skipped_tenant = uuid.uuid4()
        session = FakeDBSession(
            script=[
                _AllResult([(seeded_tenant,), (skipped_tenant,)]),
                None,
                _ScalarResult(0),  # seeded_tenant has none yet
                None,
                _ScalarResult(1),  # skipped_tenant already has one
            ]
        )

        inserted = seed_default_segments(session)

        self.assertEqual(inserted, len(DEFAULT_SEGMENTS))
        self.assertEqual({obj.tenant_id for obj in session.added}, {seeded_tenant})

    def test_concurrent_integrity_error_is_swallowed_and_rolled_back(self):
        """A concurrent process seeding the same tenant should surface as a
        harmless skip (IntegrityError on the unique tenant_id+segment_tag
        constraint), not an uncaught exception."""
        tenant_id = uuid.uuid4()
        session = FakeDBSession(
            script=[
                _AllResult([(tenant_id,)]),
                None,
                _ScalarResult(0),
            ]
        )
        session.commit_side_effect = IntegrityError("INSERT", {}, Exception("dup key"))

        inserted = seed_default_segments(session)

        self.assertEqual(inserted, 0)
        self.assertTrue(session.rolled_back)

    def test_final_generated_sql_wraps_where_clause(self):
        sql = _final_generated_sql("churn_risk_tier IN ('high', 'critical')")

        self.assertIn("cdp_master_profiles", sql)
        self.assertIn("tenant_id = :tenant_id", sql)
        self.assertIn("churn_risk_tier IN ('high', 'critical')", sql)


class InitCoreDataTests(unittest.TestCase):
    def test_init_core_data_seeds_and_closes_session(self):
        tenant_id = uuid.uuid4()
        session = FakeDBSession(
            script=[
                _AllResult([(tenant_id,)]),
                None,
                _ScalarResult(0),
            ]
        )

        with patch("core.init_core_data.SessionLocal", return_value=session):
            init_core_data()

        self.assertEqual(len(session.added), len(DEFAULT_SEGMENTS))
        self.assertTrue(session.closed)

    def test_init_core_data_swallows_unexpected_seeding_errors(self):
        """A seeding failure (e.g. DB unreachable, table missing) must never
        propagate out of init_core_data -- app startup must continue."""
        session = FakeDBSession(raise_on_call=1)

        with patch("core.init_core_data.SessionLocal", return_value=session):
            try:
                init_core_data()
            except Exception as exc:  # pragma: no cover - test failure path
                self.fail(f"init_core_data() must not raise, but raised: {exc!r}")

        self.assertTrue(session.closed)


if __name__ == "__main__":
    unittest.main()
