
-- Both MVs flatten AI/ML scoring + behavioral summaries into one table → optimized for LLM-based text-to-SQL.
-- persona_embedding is a pgvector `vector(768)` column — LLM can use it for semantic search.
-- Use CONCURRENTLY when refreshing to avoid locking reads.
-- If you expect tenant isolation, add (tenant_id, master_profile_id) composite indexes.
-- Optionally, you can create a combined MV (mv_customer_360_overview) that merges both
-- transactions + engagement in one denormalized view for super-fast semantic queries.
--
-- NOTE: these views are built strictly from the columns that actually exist in
-- core-customer360/database-schema.sql (customer360.cdp_master_profiles,
-- customer360.crm_transactions, customer360.crm_customer_contacts) -- that file is
-- the single source of truth for all table/column names used here.

-- ======================================================
-- Materialized View: Customer Transactions with AI/ML Scores
-- ======================================================
DROP MATERIALIZED VIEW IF EXISTS customer360.mv_customer_transactions CASCADE;

CREATE MATERIALIZED VIEW customer360.mv_customer_transactions AS
SELECT
    mp.master_profile_id,
    mp.tenant_id,
    mp.full_name,
    mp.email,
    mp.phone_number,
    -- AI/ML scoring fields from master profiles
    mp.engagement_score,
    mp.predictive_clv,
    mp.historical_clv,
    mp.clv_segment,
    mp.churn_probability,
    mp.churn_risk_tier,
    mp.segmentation_tags,
    mp.persona_embedding,
    -- Transaction summary fields, aggregated from crm_transactions
    COALESCE(tx.total_transactions, 0) AS total_transactions,
    tx.total_amount,
    tx.avg_transaction_amount,
    tx.last_transaction_at
FROM
    customer360.cdp_master_profiles mp
LEFT JOIN (
    SELECT
        master_profile_id,
        COUNT(*) AS total_transactions,
        SUM(amount) AS total_amount,
        AVG(amount) AS avg_transaction_amount,
        MAX(transaction_time) AS last_transaction_at
    FROM
        customer360.crm_transactions
    WHERE
        master_profile_id IS NOT NULL
    GROUP BY
        master_profile_id
) tx ON tx.master_profile_id = mp.master_profile_id
WHERE
    mp.status_code = 1;

CREATE UNIQUE INDEX idx_mv_customer_transactions_master
    ON customer360.mv_customer_transactions (master_profile_id);


-- ======================================================
-- Materialized View: Customer Engagements with AI/ML Scores
-- ======================================================
DROP MATERIALIZED VIEW IF EXISTS customer360.mv_customer_engagements CASCADE;

CREATE MATERIALIZED VIEW customer360.mv_customer_engagements AS
SELECT
    mp.master_profile_id,
    mp.tenant_id,
    mp.full_name,
    mp.email,
    mp.phone_number,
    -- AI/ML scoring fields
    mp.engagement_score,
    mp.predictive_clv,
    mp.historical_clv,
    mp.clv_segment,
    mp.churn_probability,
    mp.churn_risk_tier,
    mp.segmentation_tags,
    mp.persona_embedding,
    -- Engagement summary fields, aggregated from crm_customer_contacts
    COALESCE(cc.total_contacts, 0) AS total_contacts,
    cc.last_contact_at
FROM
    customer360.cdp_master_profiles mp
LEFT JOIN (
    SELECT
        master_profile_id,
        COUNT(*) AS total_contacts,
        MAX(contact_date) AS last_contact_at
    FROM
        customer360.crm_customer_contacts
    GROUP BY
        master_profile_id
) cc ON cc.master_profile_id = mp.master_profile_id
WHERE
    mp.status_code = 1;

CREATE UNIQUE INDEX idx_mv_customer_engagements_master
    ON customer360.mv_customer_engagements (master_profile_id);

-- ======================================================
-- Refresh Strategy
-- ======================================================
-- You can refresh materialized views periodically (cron, pgagent, Airflow, etc.)
-- Example for fast concurrent refresh:
-- REFRESH MATERIALIZED VIEW CONCURRENTLY customer360.mv_customer_transactions;
-- REFRESH MATERIALIZED VIEW CONCURRENTLY customer360.mv_customer_engagements;

