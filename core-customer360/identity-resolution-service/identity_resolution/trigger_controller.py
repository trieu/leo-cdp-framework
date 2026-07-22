"""Real-time throttle controller for Customer Identity Resolution.

Replicates, in Python, the intent of the PostgreSQL trigger
``cdp_trigger_process_new_raw_profiles`` + ``process_new_raw_profiles_trigger_func``:
after a new raw profile is ingested, try to run identity resolution, but
throttle so that concurrent/high-frequency inserts don't hammer the database.

Uses ``cdp_id_resolution_status`` (a single-row table) with
``SELECT ... FOR UPDATE NOWAIT`` for distributed locking across workers.
"""

import logging
from datetime import datetime

import psycopg2
from psycopg2.extras import RealDictCursor

from .resolver import CustomerIdentityResolver

logger = logging.getLogger(__name__)


class IdentityResolutionTrigger:
    """Throttled controller that triggers :class:`CustomerIdentityResolver`
    at most once per ``throttle_seconds`` interval, using a row-level lock on
    ``cdp_id_resolution_status`` to coordinate across concurrent workers.
    """

    def __init__(self, db_connection, schema: str = "public", throttle_seconds: int = 5):
        """
        Args:
            db_connection: A psycopg2 connection object.
            schema: The database schema containing the CDP tables.
            throttle_seconds: Minimum time (in seconds) between resolution runs.
        """
        self.conn = db_connection
        self.schema = schema
        self.throttle_seconds = throttle_seconds

    def _table(self, name: str) -> str:
        return f"{self.schema}.{name}" if self.schema else name

    def attempt_trigger(self) -> bool:
        """Checks the throttle status and executes the resolution logic if the
        minimum interval has passed.

        Catches all exceptions so that a failure here never blocks the
        calling ingestion worker (mirrors the original PL/pgSQL trigger's
        ``EXCEPTION WHEN OTHERS`` behavior).

        Returns:
            True if the resolution logic was triggered, False if throttled,
            skipped, or an error occurred.
        """
        try:
            with self.conn.cursor(cursor_factory=RealDictCursor) as cursor:
                lock_query = f"""
                    SELECT last_executed_at
                    FROM {self._table('cdp_id_resolution_status')}
                    WHERE id = TRUE
                    FOR UPDATE NOWAIT;
                """
                try:
                    cursor.execute(lock_query)
                    status_row = cursor.fetchone()
                except psycopg2.errors.LockNotAvailable:
                    logger.debug("Lock not available: another worker is currently processing.")
                    self.conn.rollback()
                    return False

                if not status_row:
                    logger.warning(
                        "Status row missing. Please initialize 'cdp_id_resolution_status'."
                    )
                    self.conn.rollback()
                    return False

                last_exec_time = status_row["last_executed_at"]
                tz_info = last_exec_time.tzinfo if last_exec_time else None
                current_time = datetime.now(tz_info)

                should_run = last_exec_time is None
                if not should_run:
                    elapsed = (current_time - last_exec_time).total_seconds()
                    should_run = elapsed >= self.throttle_seconds

                if not should_run:
                    logger.debug("Throttled: resolution ran less than %ss ago.", self.throttle_seconds)
                    self.conn.rollback()
                    return False

                logger.info("Throttle interval passed. Executing Customer Identity Resolution...")
                update_query = f"""
                    UPDATE {self._table('cdp_id_resolution_status')}
                    SET last_executed_at = NOW()
                    WHERE id = TRUE;
                """
                cursor.execute(update_query)

                resolver = CustomerIdentityResolver(db_connection=self.conn, schema=self.schema)
                # run_resolution_batch() commits internally, which also
                # commits the last_executed_at update above (same connection).
                resolver.run_resolution_batch()
                return True

        except Exception:
            logger.exception("Error in identity resolution trigger controller.")
            self.conn.rollback()
            return False
