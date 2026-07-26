"""Core Customer Identity Resolution (CIR) logic.

This is the Python OOP replacement for the PostgreSQL stored procedure
``resolve_customer_identities_dynamic`` described in
``core-customer360/identity-resolution.md``, wired up against the real,
multi-tenant ``customer360`` schema defined in
``core-customer360/database-schema.sql`` (``cdp_raw_profiles_stage``,
``cdp_master_profiles``, ``cdp_profile_links``), which stores profile data
ingested from AppsFlyer, MoEngage and Web Tracking for both the retail and
banking domains.

Matching rules are read dynamically from the ``cdp_profile_attributes``
metadata table (not part of ``database-schema.sql`` -- created on demand, see
``scripts/init_sample_data.py``).
"""

import logging
from typing import Any, Dict, List, Optional

from psycopg2.extras import Json, RealDictCursor

from .models import IdentityRule
from .persona import generate_persona_name, profile_looks_hashed

logger = logging.getLogger(__name__)

# Scalar columns present with the same name/type on both cdp_raw_profiles_stage
# and cdp_master_profiles, merged with simple COALESCE semantics.
SCALAR_MERGE_FIELDS = ("full_name", "email", "phone_number", "national_id")

# Device/marketing identifiers stored as a single column on the raw stage
# table, but consolidated into a TEXT[] identity list on the master profile
# (one raw profile only ever contributes one value; a master profile
# accumulates every distinct value it has ever been seen with).
ARRAY_IDENTITY_FIELDS = {
    "device_id": "device_ids",
    "advertising_id": "advertising_ids",
    "cookie_id": "cookie_ids",
}

# Per-source-system customer identifiers, consolidated into a JSONB map on the
# master profile: {"AppsFlyer": "...", "MoEngage": "...", ...}.
JSONB_KEYED_IDENTITY_FIELDS = {
    "external_customer_id": "external_ids",
}

RAW_PROFILE_COLUMNS = (
    "raw_profile_id",
    "tenant_id",
    "domain",
    "source_system",
    "full_name",
    "email",
    "phone_number",
    "national_id",
    "device_id",
    "advertising_id",
    "cookie_id",
    "external_customer_id",
    "push_token",
)


class CustomerIdentityResolver:
    """Links raw profiles to master profiles using dynamically configured
    matching rules stored in ``cdp_profile_attributes``, scoped per tenant
    and per domain (retail/banking).
    """

    def __init__(self, db_connection, schema: str = "customer360", batch_size: int = 1000):
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
        """Fetches a batch of raw profiles not yet processed (status_code = 1)."""
        columns = ", ".join(RAW_PROFILE_COLUMNS)
        query = f"""
            SELECT {columns}
            FROM {self._table('cdp_raw_profiles_stage')}
            WHERE status_code = 1
            LIMIT %s;
        """
        cursor.execute(query, (self.batch_size,))
        return cursor.fetchall()

    def _find_master_profile(
        self, cursor, raw_profile: Dict[str, Any], rules: List[IdentityRule]
    ) -> Optional[str]:
        """Dynamically builds and executes a query to find a matching master
        profile, scoped to the same tenant and domain, based on the active
        metadata rules."""
        conditions = []
        params: List[Any] = []
        source_system = raw_profile.get("source_system")

        for rule in rules:
            code = rule.attribute_code
            raw_value = raw_profile.get(code)
            if not raw_value:
                continue

            if code in ARRAY_IDENTITY_FIELDS:
                # Device/advertising/cookie ids: match if present in the
                # master's consolidated identity array.
                conditions.append(f"%s = ANY({ARRAY_IDENTITY_FIELDS[code]})")
                params.append(raw_value)
            elif code in JSONB_KEYED_IDENTITY_FIELDS:
                # e.g. external_customer_id: match if the master's
                # external_ids map has this exact {source_system: value} pair.
                if not source_system:
                    continue
                column = JSONB_KEYED_IDENTITY_FIELDS[code]
                conditions.append(f"{column} @> jsonb_build_object(%s::text, %s::text)")
                params.extend([source_system, raw_value])
            elif rule.match_rule == "exact":
                conditions.append(f"{code} = %s")
                params.append(raw_value)
            elif rule.match_rule == "fuzzy_trgm":
                threshold = rule.threshold if rule.threshold is not None else 0.7
                conditions.append(f"similarity({code}, %s) >= %s")
                params.extend([raw_value, threshold])
            elif rule.match_rule == "fuzzy_dmetaphone":
                conditions.append(f"dmetaphone({code}) = dmetaphone(%s)")
                params.append(raw_value)
            else:
                logger.warning(
                    "Unknown matching_rule '%s' for attribute '%s' - skipping.",
                    rule.match_rule,
                    code,
                )

        if not conditions:
            return None

        where_clause = " OR ".join(f"({c})" for c in conditions)
        query = f"""
            SELECT master_profile_id
            FROM {self._table('cdp_master_profiles')}
            WHERE tenant_id = %s AND domain = %s AND ({where_clause})
            LIMIT 1;
        """
        query_params = [raw_profile["tenant_id"], raw_profile.get("domain", "retail")] + params
        cursor.execute(query, tuple(query_params))
        result = cursor.fetchone()
        return result["master_profile_id"] if result else None

    def _link_and_update(
        self,
        cursor,
        raw_profile: Dict[str, Any],
        master_id: str,
        match_method: str = "DynamicMatch",
    ) -> None:
        """Links the raw profile to an existing master profile and merges any
        new identity data (COALESCE for scalars, append-distinct for identity
        arrays/maps)."""
        link_query = f"""
            INSERT INTO {self._table('cdp_profile_links')}
                (tenant_id, raw_profile_id, master_profile_id, match_score, match_method)
            VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (tenant_id, raw_profile_id) DO NOTHING;
        """
        cursor.execute(
            link_query,
            (
                raw_profile["tenant_id"],
                raw_profile["raw_profile_id"],
                master_id,
                1.0,
                match_method,
            ),
        )

        source_system = raw_profile.get("source_system")
        set_clauses = [f"{field} = COALESCE({field}, %s)" for field in SCALAR_MERGE_FIELDS]
        params: List[Any] = [raw_profile.get(field) for field in SCALAR_MERGE_FIELDS]

        for raw_field, master_col in ARRAY_IDENTITY_FIELDS.items():
            value = raw_profile.get(raw_field)
            if not value:
                continue
            set_clauses.append(
                f"{master_col} = CASE WHEN %s = ANY(COALESCE({master_col}, ARRAY[]::TEXT[])) "
                f"THEN {master_col} ELSE array_append(COALESCE({master_col}, ARRAY[]::TEXT[]), %s) END"
            )
            params.extend([value, value])

        if source_system:
            for raw_field, master_col in JSONB_KEYED_IDENTITY_FIELDS.items():
                value = raw_profile.get(raw_field)
                if not value:
                    continue
                set_clauses.append(
                    f"{master_col} = COALESCE({master_col}, '{{}}'::JSONB) "
                    f"|| jsonb_build_object(%s::text, %s::text)"
                )
                params.extend([source_system, value])

            push_token = raw_profile.get("push_token")
            if push_token:
                set_clauses.append(
                    "push_tokens = COALESCE(push_tokens, '{}'::JSONB) "
                    "|| jsonb_build_object(%s::text, %s::text)"
                )
                params.extend([source_system, push_token])

            set_clauses.append(
                "source_systems = CASE WHEN %s = ANY(COALESCE(source_systems, ARRAY[]::TEXT[])) "
                "THEN source_systems ELSE array_append(COALESCE(source_systems, ARRAY[]::TEXT[]), %s) END"
            )
            params.extend([source_system, source_system])

        # is_hashed/persona_name: whenever this (or an earlier) contributing
        # raw profile's PII looks SHA-256 hashed, the master profile is
        # flagged as hashed and MUST carry a persona_name (readable label
        # standing in for the now-unreadable PII) -- see persona.py and the
        # cdp_master_profiles CHECK constraint in database-schema.sql.
        looks_hashed = profile_looks_hashed(raw_profile)
        set_clauses.append("is_hashed = is_hashed OR %s")
        params.append(looks_hashed)
        set_clauses.append("persona_name = COALESCE(persona_name, %s)")
        params.append(generate_persona_name(raw_profile) if looks_hashed else None)

        set_clauses.append("updated_at = NOW()")
        params.extend([master_id, raw_profile["tenant_id"]])

        update_query = f"""
            UPDATE {self._table('cdp_master_profiles')}
            SET {", ".join(set_clauses)}
            WHERE master_profile_id = %s AND tenant_id = %s;
        """
        cursor.execute(update_query, tuple(params))

    def _create_master_and_link(self, cursor, raw_profile: Dict[str, Any]) -> str:
        """Creates a brand new master profile when no match was found and
        links the raw profile to it. Returns the new master_profile_id."""
        source_system = raw_profile.get("source_system")

        external_ids = {}
        if source_system and raw_profile.get("external_customer_id"):
            external_ids[source_system] = raw_profile["external_customer_id"]

        push_tokens = {}
        if source_system and raw_profile.get("push_token"):
            push_tokens[source_system] = raw_profile["push_token"]

        device_ids = [raw_profile["device_id"]] if raw_profile.get("device_id") else []
        advertising_ids = [raw_profile["advertising_id"]] if raw_profile.get("advertising_id") else []
        cookie_ids = [raw_profile["cookie_id"]] if raw_profile.get("cookie_id") else []
        source_systems = [source_system] if source_system else []

        # is_hashed/persona_name: see the matching comment in _link_and_update.
        looks_hashed = profile_looks_hashed(raw_profile)
        persona_name = generate_persona_name(raw_profile) if looks_hashed else None

        insert_master_query = f"""
            INSERT INTO {self._table('cdp_master_profiles')}
                (tenant_id, domain, full_name, email, phone_number, national_id,
                 external_ids, device_ids, advertising_ids, cookie_ids, push_tokens,
                 source_systems, first_seen_raw_profile_id, is_hashed, persona_name)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
            RETURNING master_profile_id;
        """
        cursor.execute(
            insert_master_query,
            (
                raw_profile["tenant_id"],
                raw_profile.get("domain", "retail"),
                raw_profile.get("full_name"),
                raw_profile.get("email"),
                raw_profile.get("phone_number"),
                raw_profile.get("national_id"),
                Json(external_ids),
                device_ids,
                advertising_ids,
                cookie_ids,
                Json(push_tokens),
                source_systems,
                raw_profile["raw_profile_id"],
                looks_hashed,
                persona_name,
            ),
        )
        new_master_id = cursor.fetchone()["master_profile_id"]

        link_query = f"""
            INSERT INTO {self._table('cdp_profile_links')}
                (tenant_id, raw_profile_id, master_profile_id, match_score, match_method)
            VALUES (%s, %s, %s, %s, %s);
        """
        cursor.execute(
            link_query,
            (raw_profile["tenant_id"], raw_profile["raw_profile_id"], new_master_id, 1.0, "NewMaster"),
        )
        return new_master_id

    def _mark_as_processed(self, cursor, raw_profile_id: str) -> None:
        """Marks a raw profile as processed (status_code = 3) and stamps processed_at."""
        query = f"""
            UPDATE {self._table('cdp_raw_profiles_stage')}
            SET status_code = 3, processed_at = NOW()
            WHERE raw_profile_id = %s;
        """
        cursor.execute(query, (raw_profile_id,))

    def run_resolution_batch(self) -> int:
        """Main orchestration method for a single batch run. Idempotent and
        safe to retry: only rows with ``status_code = 1`` are touched, and
        links are protected by the unique constraint on
        ``(tenant_id, raw_profile_id)``.

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
