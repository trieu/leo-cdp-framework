"""Unit tests for core.utils.sql_safety -- the injection-safety validation
applied to CdpSegment.sql_rules / final_generated_sql, both at write time
(core/schemas/segmentation.py) and again immediately before every execution
(core/routers/segment.py).
"""

import unittest

from core.utils.sql_safety import validate_readonly_sql_statement, validate_sql_where_fragment


class ValidateSqlWhereFragmentTests(unittest.TestCase):
    def test_accepts_simple_comparison(self):
        self.assertEqual(validate_sql_where_fragment("predictive_clv > 1000"), "predictive_clv > 1000")

    def test_accepts_in_clause(self):
        fragment = "churn_risk_tier IN ('high', 'critical')"
        self.assertEqual(validate_sql_where_fragment(fragment), fragment)

    def test_accepts_interval_expression(self):
        fragment = "customer_since >= (CURRENT_DATE - INTERVAL '30 days')"
        self.assertEqual(validate_sql_where_fragment(fragment), fragment)

    def test_rejects_empty_string(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment("")

    def test_rejects_none(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment(None)

    def test_rejects_statement_stacking_semicolon(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment("1=1; DROP TABLE cdp_master_profiles;")

    def test_rejects_sql_comment(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment("1=1 -- OR tenant_id = 'x'")

    def test_rejects_block_comment(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment("1=1 /* sneaky */ OR 1=1")

    def test_rejects_dml_keyword(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment("1=1); DELETE FROM sys_user WHERE (1=1")

    def test_rejects_ddl_keyword(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment("1=1) OR (DROP TABLE sys_tenant")

    def test_rejects_subquery_select(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment("tenant_id IN (SELECT tenant_id FROM sys_tenant)")

    def test_rejects_union(self):
        with self.assertRaises(ValueError):
            validate_sql_where_fragment("1=1 UNION SELECT * FROM sys_user")


class ValidateReadonlySqlStatementTests(unittest.TestCase):
    def test_allows_select_from(self):
        sql = "SELECT master_profile_id FROM customer360.cdp_master_profiles WHERE tenant_id = :tenant_id"
        self.assertEqual(validate_readonly_sql_statement(sql), sql)

    def test_allows_none_and_empty(self):
        self.assertIsNone(validate_readonly_sql_statement(None))
        self.assertEqual(validate_readonly_sql_statement(""), "")

    def test_rejects_statement_stacking(self):
        with self.assertRaises(ValueError):
            validate_readonly_sql_statement("SELECT 1; DROP TABLE cdp_segments;")

    def test_rejects_dml_keyword(self):
        with self.assertRaises(ValueError):
            validate_readonly_sql_statement("SELECT 1 WHERE 1=1 OR (DELETE FROM sys_user)=1")


if __name__ == "__main__":
    unittest.main()
