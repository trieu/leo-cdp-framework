"""Unit tests for identity_resolution.trigger_controller.IdentityResolutionTrigger.

No real database is used: psycopg2 connection/cursor objects are mocked via
the `mock_conn` / `mock_cursor` fixtures in conftest.py.
"""

from datetime import datetime, timedelta, timezone
from unittest.mock import patch

import psycopg2

from identity_resolution.trigger_controller import IdentityResolutionTrigger


class TestAttemptTrigger:
    def test_runs_resolution_when_never_executed_before(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = {"last_executed_at": None}
        trigger = IdentityResolutionTrigger(mock_conn, schema="public", throttle_seconds=5)

        with patch("identity_resolution.trigger_controller.CustomerIdentityResolver") as mock_resolver_cls:
            result = trigger.attempt_trigger()

        assert result is True
        mock_resolver_cls.assert_called_once_with(db_connection=mock_conn, schema="public")
        mock_resolver_cls.return_value.run_resolution_batch.assert_called_once()

    def test_runs_resolution_when_throttle_window_elapsed(self, mock_cursor, mock_conn):
        last_exec = datetime.now(timezone.utc) - timedelta(seconds=100)
        mock_cursor.fetchone.return_value = {"last_executed_at": last_exec}
        trigger = IdentityResolutionTrigger(mock_conn, schema="public", throttle_seconds=5)

        with patch("identity_resolution.trigger_controller.CustomerIdentityResolver") as mock_resolver_cls:
            result = trigger.attempt_trigger()

        assert result is True
        mock_resolver_cls.return_value.run_resolution_batch.assert_called_once()

    def test_throttled_when_run_recently(self, mock_cursor, mock_conn):
        last_exec = datetime.now(timezone.utc) - timedelta(seconds=1)
        mock_cursor.fetchone.return_value = {"last_executed_at": last_exec}
        trigger = IdentityResolutionTrigger(mock_conn, schema="public", throttle_seconds=5)

        with patch("identity_resolution.trigger_controller.CustomerIdentityResolver") as mock_resolver_cls:
            result = trigger.attempt_trigger()

        assert result is False
        mock_resolver_cls.assert_not_called()
        mock_conn.rollback.assert_called_once()

    def test_returns_false_when_lock_not_available(self, mock_cursor, mock_conn):
        mock_cursor.execute.side_effect = psycopg2.errors.LockNotAvailable("locked")
        trigger = IdentityResolutionTrigger(mock_conn, schema="public")

        result = trigger.attempt_trigger()

        assert result is False
        mock_conn.rollback.assert_called_once()

    def test_returns_false_when_status_row_missing(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.return_value = None
        trigger = IdentityResolutionTrigger(mock_conn, schema="public")

        result = trigger.attempt_trigger()

        assert result is False
        mock_conn.rollback.assert_called_once()

    def test_returns_false_and_rolls_back_on_unexpected_error(self, mock_cursor, mock_conn):
        mock_cursor.fetchone.side_effect = RuntimeError("db exploded")
        trigger = IdentityResolutionTrigger(mock_conn, schema="public")

        result = trigger.attempt_trigger()

        assert result is False
        mock_conn.rollback.assert_called_once()
