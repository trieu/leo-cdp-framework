"""Core Customer Identity Resolution (CIR) logic.

This module is the Python OOP replacement for the PostgreSQL stored
procedure ``resolve_customer_identities_dynamic`` documented in
``core-customer360/identity-resolution.md``. It reads matching rules
dynamically from the ``cdp_profile_attributes`` metadata table and links /
merges rows from ``cdp_raw_profiles_stage`` into ``cdp_master_profiles``.

Column names below intentionally mirror the DDL in
``core-customer360/identity-resolution.md`` (``cdp_raw_profiles_stage``,
``cdp_master_profiles``, ``cdp_profile_links``, ``cdp_profile_attributes``) so
this class can run against that schema unmodified.
"""

import logging
from typing import Any, Dict, List, Optional

from psycopg2.extras import RealDictCursor

from .models import IdentityRule

logger = logging.getLogger(__name__)

# Columns present on both cdp_raw_profiles_stage and cdp_master_profiles that
# get merged (COALESCE'd) into the master record.
MERGEABLE_FIELDS = (
    "first_name",
    "last_name",
    "email",
    "phone_number",
    "address_line1",
    "city",
    "state",
    "zip_code",
)

VALID_MATCH_RULES = ("exact", "fuzzy_trgm", "fuzzy_dmetaphone")


class CustomerIdentityResolver:
    """Links raw profiles to master profiles using dynamically configured
    matching rules stored in ``cdp_profile_attributes``.
    """

    def __init__(self, db_connection, schema: str = "public", batch_size: int = 1000):
        """
        Args:
            db_connection: A psycopg2 connection object (or a connection pool
                checkout), injected for easy unit testing.
            schema: The database schema containing the CDP tables.
            batch_size: Number of records to process per batch/run.
        """
        self.conn = db_connection
        self.schema = schema
        self.batch_size = batch_size

    def _table(self, name: str) -> str:
        return f"{self.schema}.{name}" if self.schema else name

    def _get_active_rules(self, cursor) -> List[IdentityRule]:
        """Fetches active identity resolution rules from the metadata table."""
        query = f"""
            SELECT attribute_internal_code, matching_rule, matching_threshold
            FROM {self._table('cdp_profile_attributes')}
            WHERE is_identity_resolution = TRUE
              AND status = 'ACTIVE'
              AND matching_rule IS NOT NULL
              AND matching_rule != 'none';
        """
        cursor.execute(query)
        return [
            IdentityRule(
                attribute_code=row["attribute_internal_code"],
                match_rule=row["matching_rule"],
                threshold=row["matching_threshold"],
            )
            for row in cursor.fetchall()
        ]

    def _fetch_unprocessed_profiles(self, cursor) -> List[Dict[str, Any]]:
        """Fetches a batch of raw profiles not yet processed (processed_at IS NULL)."""
        query = f"""
            SELECT raw_profile_id, first_name, last_name, email, phone_number,
                   address_line1, city, state, zip_code, source_system
            FROM {self._table('cdp_raw_profiles_stage')}
            WHERE processed_at IS NULL
            LIMIT %s;
        """
        cursor.execute(query, (self.batch_size,))
        return cursor.fetchall()

    def _find_master_profile(
        self, cursor, raw_profile: Dict[str, Any], rules: List[IdentityRule]
    ) -> Optional[str]:
        """Dynamically builds and executes a query to find a matching master
        profile based on the active metadata rules."""
        conditions = []
        params: List[Any] = []

        for rule in rules:
            raw_value = raw_profile.get(rule.attribute_code)
            if not raw_value:
                continue

            col_name = rule.attribute_code

            if rule.match_rule == "exact":
                conditions.append(f"{col_name} = %s")
                params.append(raw_value)
            elif rule.match_rule == "fuzzy_trgm":
                threshold = rule.threshold if rule.threshold is not None else 0.7
                conditions.append(f"similarity({col_name}, %s) >= %s")
                params.extend([raw_value, threshold])
            elif rule.match_rule == "fuzzy_dmetaphone":
                conditions.append(f"dmetaphone({col_name}) = dmetaphone(%s)")
                params.append(raw_value)
            else:
                logger.warning(
                    "Unknown matching_rule '%s' for attribute '%s' - skipping.",
                    rule.match_rule,
                    col_name,
                )

        if not conditions:
            return None

        where_clause = " OR ".join(f"({c})" for c in conditions)
        query = f"""
            SELECT master_profile_id
            FROM {self._table('cdp_master_profiles')}
            WHERE {where_clause}
            LIMIT 1;
        """
        cursor.execute(query, tuple(params))
        result = cursor.fetchone()
        return result["master_profile_id"] if result else None

    def _link_and_update(
        self,
        cursor,
        raw_profile: Dict[str, Any],
        master_id: str,
        match_rule: str = "DynamicMatch",
    ) -> None:
        """Links the raw profile to an existing master profile and fills any
        gaps on the master record (COALESCE semantics)."""
        link_query = f"""
            INSERT INTO {self._table('cdp_profile_links')}
                (raw_profile_id, master_profile_id, match_rule)
            VALUES (%s, %s, %s)
            ON CONFLICT (raw_profile_id) DO NOTHING;
        """
        cursor.execute(link_query, (raw_profile["raw_profile_id"], master_id, match_rule))

        set_clause = ", ".join(f"{field} = COALESCE({field}, %s)" for field in MERGEABLE_FIELDS)
        update_query = f"""
            UPDATE {self._table('cdp_master_profiles')}
            SET {set_clause},
                source_systems = array_append(
                    COALESCE(source_systems, ARRAY[]::TEXT[]), %s::TEXT
                ),
                updated_at = NOW()
            WHERE master_profile_id = %s
              AND NOT (%s = ANY(COALESCE(source_systems, ARRAY[]::TEXT[])));
        """
        source_system = raw_profile.get("source_system")
        params = tuple(raw_profile.get(field) for field in MERGEABLE_FIELDS) + (
            source_system,
            master_id,
            source_system,
        )
        cursor.execute(update_query, params)

    def _create_master_and_link(self, cursor, raw_profile: Dict[str, Any]) -> str:
        """Creates a brand new master profile when no match was found and
        links the raw profile to it. Returns the new master_profile_id."""
        insert_master_query = f"""
            INSERT INTO {self._table('cdp_master_profiles')}
                (first_name, last_name, email, phone_number, address_line1,
                 city, state, zip_code, first_seen_raw_profile_id, source_systems)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, ARRAY[%s]::TEXT[])
            RETURNING master_profile_id;
        """
        params = tuple(raw_profile.get(field) for field in MERGEABLE_FIELDS) + (
            raw_profile["raw_profile_id"],
            raw_profile.get("source_system"),
        )
        cursor.execute(insert_master_query, params)
        new_master_id = cursor.fetchone()["master_profile_id"]

        link_query = f"""
            INSERT INTO {self._table('cdp_profile_links')}
                (raw_profile_id, master_profile_id, match_rule)
            VALUES (%s, %s, %s);
        """
        cursor.execute(link_query, (raw_profile["raw_profile_id"], new_master_id, "NewMaster"))
        return new_master_id

    def _mark_as_processed(self, cursor, raw_profile_id: str) -> None:
        """Marks a raw profile as processed by stamping processed_at."""
        query = f"""
            UPDATE {self._table('cdp_raw_profiles_stage')}
            SET processed_at = NOW()
            WHERE raw_profile_id = %s;
        """
        cursor.execute(query, (raw_profile_id,))

    def run_resolution_batch(self) -> int:
        """Main orchestration method for a single batch run. Idempotent and
        safe to retry: only rows with ``processed_at IS NULL`` are touched,
        and links are protected by the unique constraint on ``raw_profile_id``.

        Returns:
            The number of raw profiles processed in this batch.
        """
        try:
            with self.conn.cursor(cursor_factory=RealDictCursor) as cursor:
                rules = self._get_active_rules(cursor)
                if not rules:
                    logger.warning("No active identity resolution rules found. Aborting.")
                    return 0

                raw_profiles = self._fetch_unprocessed_profiles(cursor)
                if not raw_profiles:
                    logger.info("No unprocessed profiles found in staging.")
                    return 0

                logger.info("Processing batch of %d profiles.", len(raw_profiles))

                for profile in raw_profiles:
                    matched_id = self._find_master_profile(cursor, profile, rules)

                    if matched_id:
                        self._link_and_update(cursor, profile, matched_id)
                    else:
                        self._create_master_and_link(cursor, profile)

                    self._mark_as_processed(cursor, profile["raw_profile_id"])

                self.conn.commit()
                logger.info("Batch processed and committed successfully.")
                return len(raw_profiles)

        except Exception:
            self.conn.rollback()
            logger.exception("Error during identity resolution.")
            raise
