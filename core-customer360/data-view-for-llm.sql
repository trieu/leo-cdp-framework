
-- Both MVs flatten AI/ML scoring + behavioral summaries into one table → optimized for LLM-based text-to-SQL.
-- embedding_vector can be stored as vector (pgvector) — LLM can use it for semantic search.
-- Use CONCURRENTLY when refreshing to avoid locking reads.
-- If you expect tenant isolation, add (tenant_id, master_profile_id) composite indexes.
-- Optionally, you can create combined MV (mv_customer_360_overview) that merges both purchases + engagement in one denormalized view for super-fast semantic queries.

-- ======================================================
-- Materialized View: Customer Purchases with AI/ML Scores
-- ======================================================
DROP MATERIALIZED VIEW IF EXISTS mv_customer_purchases CASCADE;

CREATE MATERIALIZED VIEW mv_customer_purchases AS
SELECT
    mp.master_profile_id,
    mp.tenant_id,
    mp.full_name,
    mp.email,
    mp.phone_number,
    -- AI/ML scoring fields from master profiles
    mp.engagement_score,
    mp.clv_score,
    mp.churn_risk_score,
    mp.segment,
    mp.embedding_vector,
    -- Purchase summary fields
    mp.total_purchases,
    mp.total_spent,
    mp.avg_order_value,
    mp.last_purchase_date
FROM
    cdp_master_profiles mp
WHERE
    mp.status = 'active';

CREATE UNIQUE INDEX idx_mv_customer_purchases_master
    ON mv_customer_purchases (master_profile_id);


-- ======================================================
-- Materialized View: Customer Engagements with AI/ML Scores
-- ======================================================
DROP MATERIALIZED VIEW IF EXISTS mv_customer_engagements CASCADE;

CREATE MATERIALIZED VIEW mv_customer_engagements AS
SELECT
    mp.master_profile_id,
    mp.tenant_id,
    mp.full_name,
    mp.email,
    mp.phone_number,
    -- AI/ML scoring fields
    mp.engagement_score,
    mp.clv_score,
    mp.churn_risk_score,
    mp.segment,
    mp.embedding_vector,
    -- Engagement summary fields
    mp.total_engagements,
    mp.avg_engagements_per_month,
    mp.last_engagement_date
FROM
    cdp_master_profiles mp
WHERE
    mp.status = 'active';

CREATE UNIQUE INDEX idx_mv_customer_engagements_master
    ON mv_customer_engagements (master_profile_id);

-- ======================================================
-- Refresh Strategy
-- ======================================================
-- You can refresh materialized views periodically (cron, pgagent, Airflow, etc.)
-- Example for fast concurrent refresh:
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_purchases;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY mv_customer_engagements;
