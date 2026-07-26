"""Safety net for executing admin-authored SQL fragments/statements stored on
``cdp_segments`` (see core/models/segmentation.py).

``sql_rules`` is a WHERE-clause fragment translated client-side (by jQuery
QueryBuilder's ``getSQL()``) from a structured rule tree and stored verbatim
-- there's no way to safely bind-parameterize an arbitrary user-composed
boolean expression, so instead of parameterizing it we apply strict
allow-list-style validation before ever interpolating it into a query: no
statement separators/comments (blocks stacked-query injection) and no DML/
DDL/query keywords. ``final_generated_sql`` is a full (never executed by this
API -- informational/audit only) SELECT statement, so it's checked with a
looser variant that still blocks statement stacking and DML/DDL but allows
SELECT/FROM/JOIN.

This is defense-in-depth, not a full SQL parser. It's applied both when a
segment is created/updated (core/schemas/segmentation.py) AND again
immediately before every execution (core/routers/segment.py), so rows
seeded/migrated outside the API (e.g. core/init_core_data.py, which builds
ORM objects directly and never goes through the Pydantic schemas) are still
checked at execution time.
"""

import re

# Statement stacking / comment-based injection -- never legitimate in either
# a WHERE fragment or a single read-only SELECT statement.
_STACKING_OR_COMMENT_PATTERN = re.compile(r";|--|/\*|\*/")

# DML/DDL/session/administrative keywords -- never legitimate in a segment
# rule, whether it's a fragment or a full statement.
_DML_DDL_KEYWORDS = (
    r"\b(insert|update|delete|drop|alter|grant|revoke|truncate|create|exec|execute|call|copy|"
    r"vacuum|reindex|pg_sleep|xp_cmdshell|dblink|set|reset)\b"
)
_DML_DDL_PATTERN = re.compile(_DML_DDL_KEYWORDS, re.IGNORECASE)

# For a WHERE-clause *fragment* specifically (sql_rules), SELECT/FROM/JOIN/
# UNION/INTO have no legitimate use either -- their presence strongly
# suggests a subquery/stacked-query injection attempt.
_QUERY_KEYWORDS_PATTERN = re.compile(r"\b(select|from|join|union|into)\b", re.IGNORECASE)


def validate_sql_where_fragment(fragment: str) -> str:
    """Returns ``fragment`` unchanged if it looks like a single, safe boolean
    WHERE-clause expression (e.g. ``"churn_risk_tier IN ('high', 'critical')"``),
    else raises ``ValueError``."""
    if not fragment or not fragment.strip():
        raise ValueError("sql_rules must not be empty")
    if _STACKING_OR_COMMENT_PATTERN.search(fragment):
        raise ValueError("sql_rules must not contain statement separators (;) or comments (--, /* */)")
    if _DML_DDL_PATTERN.search(fragment):
        raise ValueError("sql_rules must not contain DML/DDL/administrative SQL keywords")
    if _QUERY_KEYWORDS_PATTERN.search(fragment):
        raise ValueError("sql_rules must be a single WHERE-clause expression (no SELECT/FROM/JOIN/UNION)")
    return fragment


def validate_readonly_sql_statement(sql: str) -> str:
    """Looser validation for ``final_generated_sql`` (a full, human-readable
    SELECT statement kept for reference -- never executed by this API):
    blocks statement stacking/comments and DML/DDL, but allows SELECT/FROM/
    JOIN/UNION since those are expected here."""
    if not sql or not sql.strip():
        return sql
    if _STACKING_OR_COMMENT_PATTERN.search(sql):
        raise ValueError("final_generated_sql must not contain statement separators (;) or comments (--, /* */)")
    if _DML_DDL_PATTERN.search(sql):
        raise ValueError("final_generated_sql must not contain DML/DDL/administrative SQL keywords")
    return sql
