"""Shared test fixtures/fakes for the customer360-api unit test suite.

No real PostgreSQL/Redis/Keycloak is required to run these tests -- every
external dependency (DB session, Redis client, Keycloak introspection) is
faked/mocked so the suite is fast and hermetic. See ./run_unit_tests.sh.
"""

from typing import Any, Optional

import pytest


class FakeRedis:
    """Minimal in-memory stand-in for redis.Redis, covering get/set/scan/delete."""

    def __init__(self):
        self.store: dict[str, str] = {}

    def get(self, key: str) -> Optional[str]:
        return self.store.get(key)

    def set(self, key: str, value: str, ex: Optional[int] = None) -> None:
        self.store[key] = value

    def scan(self, cursor: int = 0, match: str = "*", count: int = 100):
        import fnmatch

        keys = [k for k in self.store if fnmatch.fnmatch(k, match)]
        return 0, keys

    def delete(self, *keys: str) -> None:
        for k in keys:
            self.store.pop(k, None)


class FakeQueryResult:
    """Stands in for a SQLAlchemy CursorResult, supporting `.mappings().first()`."""

    def __init__(self, row: Optional[dict[str, Any]]):
        self._row = row

    def mappings(self) -> "FakeQueryResult":
        return self

    def first(self) -> Optional[dict[str, Any]]:
        return self._row


class FakeDBSession:
    """Stands in for a SQLAlchemy Session, scripted with canned results.

    ``script`` is a list of return values (plain values or FakeQueryResult)
    handed out in order, one per ``execute()`` call. Every executed
    statement/params pair is recorded in ``executed`` for assertions.
    """

    def __init__(self, script: Optional[list[Any]] = None, raise_on_call: Optional[int] = None):
        self.script = list(script or [])
        self.raise_on_call = raise_on_call
        self.executed: list[tuple[str, Optional[dict[str, Any]]]] = []
        self.added: list[Any] = []
        self.committed = False
        self.rolled_back = False
        self.closed = False
        self._call_count = 0

    def execute(self, stmt: Any, params: Optional[dict[str, Any]] = None) -> Any:
        self._call_count += 1
        self.executed.append((str(stmt), params))
        if self.raise_on_call is not None and self._call_count == self.raise_on_call:
            raise RuntimeError("simulated database error")
        if self.script:
            return self.script.pop(0)
        return FakeQueryResult(None)

    def add(self, obj: Any) -> None:
        """Stands in for Session.add() -- used by ORM-style code (e.g.
        core.init_core_data.seed_default_segments) that builds model
        instances and adds them to the session instead of calling execute()
        directly. commit()/rollback() below can be scripted to raise via
        commit_side_effect to simulate IntegrityError races."""
        self.added.append(obj)

    def commit(self) -> None:
        if getattr(self, "commit_side_effect", None) is not None:
            effect = self.commit_side_effect
            self.commit_side_effect = None
            raise effect
        self.committed = True

    def rollback(self) -> None:
        self.rolled_back = True

    def close(self) -> None:
        self.closed = True


@pytest.fixture
def fake_redis() -> FakeRedis:
    return FakeRedis()


@pytest.fixture
def fake_db_session_factory():
    """Returns a factory: call it with a script list to get a callable
    (mimicking SessionLocal()) that always returns the same FakeDBSession."""

    def _factory(script: Optional[list[Any]] = None, raise_on_call: Optional[int] = None) -> FakeDBSession:
        return FakeDBSession(script=script, raise_on_call=raise_on_call)

    return _factory
