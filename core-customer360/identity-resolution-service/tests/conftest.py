"""Pytest fixtures for identity_resolution unit tests.

All tests mock the psycopg2 connection/cursor so no real PostgreSQL
instance is required to run the test suite.
"""

from unittest.mock import MagicMock

import pytest


@pytest.fixture
def mock_cursor():
    """A MagicMock standing in for a psycopg2 (RealDict) cursor."""
    return MagicMock()


@pytest.fixture
def mock_conn(mock_cursor):
    """A MagicMock connection whose `.cursor(...)` context manager yields
    `mock_cursor`."""
    conn = MagicMock()
    cursor_ctx = MagicMock()
    cursor_ctx.__enter__.return_value = mock_cursor
    cursor_ctx.__exit__.return_value = False
    conn.cursor.return_value = cursor_ctx
    return conn
