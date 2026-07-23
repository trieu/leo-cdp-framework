-- =========================================================
-- Extensions
-- =========================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS vector;

-- =========================================================
-- Schema
-- =========================================================
CREATE SCHEMA IF NOT EXISTS customer360;

---------------------------------------------------
-- ENTITY TABLES
---------------------------------------------------

-- Campaign
CREATE TABLE customer360.campaign (
  campaign_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  keywords TEXT[],
  lang TEXT DEFAULT 'en',
  embedding vector(1536),
  start_date DATE,
  end_date DATE,
  metadata JSONB,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- CampaignMember
CREATE TABLE customer360.campaign_member (
  campaign_member_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  campaign_id UUID REFERENCES customer360.campaign(campaign_id),
  contact_id UUID,
  status TEXT,
  description TEXT,
  keywords TEXT[],
  lang TEXT DEFAULT 'en',
  embedding vector(1536),
  joined_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  metadata JSONB
);

-- Lead
CREATE TABLE customer360.lead (
  lead_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  first_name TEXT,
  last_name TEXT,
  email TEXT,
  phone TEXT,
  description TEXT,
  keywords TEXT[],
  lang TEXT DEFAULT 'en',
  embedding vector(1536),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  metadata JSONB
);

-- Lead Source
CREATE TABLE customer360.lead_source (
  lead_source_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  keywords TEXT[],
  lang TEXT DEFAULT 'en',
  embedding vector(1536),
  metadata JSONB
);

-- Contact
CREATE TABLE customer360.contact (
  contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  first_name TEXT,
  last_name TEXT,
  email TEXT,
  phone TEXT,
  account_id UUID,
  description TEXT,
  keywords TEXT[],
  lang TEXT DEFAULT 'en',
  embedding vector(1536),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  metadata JSONB
);

-- Account
CREATE TABLE customer360.account (
  account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  industry_id UUID,
  description TEXT,
  keywords TEXT[],
  lang TEXT DEFAULT 'en',
  embedding vector(1536),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  metadata JSONB
);

-- Opportunity
CREATE TABLE customer360.opportunity (
  opportunity_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  account_id UUID REFERENCES customer360.account(account_id),
  name TEXT,
  value NUMERIC,
  stage TEXT,
  close_date DATE,
  description TEXT,
  keywords TEXT[],
  lang TEXT DEFAULT 'en',
  embedding vector(1536),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  metadata JSONB
);

-- Industry
CREATE TABLE customer360.industry (
  industry_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  description TEXT,
  keywords TEXT[],
  lang TEXT DEFAULT 'en',
  embedding vector(1536),
  metadata JSONB
);

---------------------------------------------------
-- MASTER PROFILES & IDENTITY RESOLUTION
---------------------------------------------------

-- ============================================================================
-- LEO CDP MASTER PROFILE SCHEMA (PostgreSQL 16+)
-- ============================================================================
-- Description: Golden customer profile containing the consolidated ("resolved") 
-- identity across multiple data sources (AppsFlyer, MoEngage, Web Tracking / GA4, 
-- POS, Core Banking, etc.) for both retail and banking domains.
-- ============================================================================

CREATE TABLE customer360.cdp_master_profiles (
    -- ------------------------------------------------------------------------
    -- SYSTEM & TENANT METADATA
    -- ------------------------------------------------------------------------
    master_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- Multi-tenancy support. Ensures data isolation between different workspaces.
    tenant_id UUID NOT NULL,
    -- Business context of the profile to drive domain-specific UI and activation logic.
    domain TEXT NOT NULL DEFAULT 'retail' CHECK (domain IN ('retail', 'banking')),

    -- ------------------------------------------------------------------------
    -- CORE IDENTITY (PII & DEMOGRAPHICS)
    -- Standard demographic data used for personalization and primary matching.
    -- ------------------------------------------------------------------------
    full_name TEXT,
    first_name TEXT,
    last_name TEXT,
    profile_picture_url TEXT,
    -- True if full_name/email/phone_number/national_id are SHA-256 hashed for privacy
    -- (e.g. hashed-match ingestion a la Meta/Google Customer Match). Whenever TRUE,
    -- persona_name (below) MUST be populated -- see the CHECK constraint at the end of
    -- this table -- since hashed PII can no longer be used as a human-readable label for
    -- browsing/semantic search. persona_name is computed by application code (see
    -- identity-resolution-service/identity_resolution/persona.py), never by the DB.
    is_hashed BOOLEAN DEFAULT FALSE,
    
    -- Primary contact info (used for primary identity stitching and marketing)
    email TEXT,
    phone_number TEXT,
    
    -- Secondary contact info 
    -- Format: [{"email": "work@abc.com", "label": "work"}, {"email": "old@xyz.com", "label": "personal"}]
    secondary_emails JSONB DEFAULT '[]'::JSONB,
    -- Format: [{"phone": "+84901234567", "label": "home"}]
    secondary_phones JSONB DEFAULT '[]'::JSONB,
    
    date_of_birth DATE,
    gender TEXT CHECK (gender IN ('male','female','other')),
    -- Flexible JSON document for complex address storage 
    -- Format: {"street": "123 Le Loi", "city": "Ho Chi Minh", "country": "VN"}
    address JSONB,

    -- ------------------------------------------------------------------------
    -- CROSS-CHANNEL IDENTITY GRAPH
    -- Identifiers resolved and merged from cdp_raw_profiles_stage.
    -- ------------------------------------------------------------------------
    -- Maps a source_system to its own customer identifier (Deterministic matching).
    external_ids JSONB DEFAULT '{}'::JSONB,
    -- Hardware or app-specific identifiers for mobile attribution (IDFV, Android ID).
    device_ids TEXT[] DEFAULT ARRAY[]::TEXT[],
    -- Mobile advertising identifiers for retargeting campaigns (AppsFlyer IDFA/GAID).
    advertising_ids TEXT[] DEFAULT ARRAY[]::TEXT[],
    -- Anonymous browser cookies for web tracking and session stitching.
    cookie_ids TEXT[] DEFAULT ARRAY[]::TEXT[],
    -- Stored tokens for push notification services (MoEngage, Firebase).
    -- Format: {"fcm": "token_string", "apns": "token_string"}
    push_tokens JSONB DEFAULT '{}'::JSONB,

    -- ------------------------------------------------------------------------
    -- RETAIL DOMAIN ATTRIBUTES
    -- Fields specific to e-commerce, POS, and physical retail operations.
    -- ------------------------------------------------------------------------
    loyalty_id TEXT,
    membership_tier TEXT,
    preferred_store_code TEXT,

    -- ------------------------------------------------------------------------
    -- BANKING DOMAIN ATTRIBUTES
    -- Highly regulated fields specific to Fintech and Core Banking systems.
    -- ------------------------------------------------------------------------
    -- National Identification (CMND/CCCD in Vietnam, or Passport number).
    national_id TEXT,
    -- Core Banking Customer Information File number. The golden record ID in legacy banking.
    cif_number TEXT,
    -- Array of active account numbers associated with this CIF.
    account_numbers TEXT[] DEFAULT ARRAY[]::TEXT[],
    -- Know Your Customer (eKYC/KYC) progression state.
    kyc_status TEXT CHECK (kyc_status IN ('unverified','pending','verified','rejected')),
    -- Risk categorization for AML or credit scoring.
    risk_segment TEXT,

    -- ------------------------------------------------------------------------
    -- MARKETING & ENGAGEMENT
    -- Attribution data and computed fields used for audience building.
    -- ------------------------------------------------------------------------
    -- Persona Name for segmentation, marketing campaigns and semantic search (e.g., "Gen Z
    -- Shopper", "High-Value Investor"). REQUIRED whenever is_hashed = TRUE: once real PII is
    -- SHA-256 hashed, persona_name is the only human-readable label left to browse/search
    -- profiles by, so it is auto-generated by identity-resolution-service (see persona.py)
    -- for every profile whose PII looks hashed.
    persona_name TEXT default 'anonymous_user',
    -- First-touch channel attribution (e.g., 'organic_search', 'paid_social').
    acquisition_source TEXT,
    -- First-touch campaign attribution.
    acquisition_campaign TEXT,
    -- Stored embeddings generated by LLMs for semantic semantic search/lookalike modeling.
    persona_embedding vector(768),
    -- Computed labels for fast Audience Builder queries (e.g., 'gen_z', 'frequent_buyer').
    segmentation_tags TEXT[],
    -- Schemaless payload for flexible traits extracted dynamically.
    -- Format: {"occupation": "engineer", "income_segment": "high", "preferred_category": "electronics"}
    attributes JSONB DEFAULT '{}'::JSONB,
    
    -- ------------------------------------------------------------------------
    -- LINEAGE & AUDIT
    -- ------------------------------------------------------------------------
    -- Array of all external systems that have contributed data to this profile.
    source_systems TEXT[] DEFAULT ARRAY[]::TEXT[],
    -- Lineage pointer back to the raw_profile_id that initiated this profile.
    first_seen_raw_profile_id UUID,

    -- ------------------------------------------------------------------------
    -- 🚀 ML & ANALYTICS SCORING MODELS
    -- Computed asynchronously by data pipelines / ML models.
    -- ------------------------------------------------------------------------

    -- 1. Lead & Conversion Scoring
    -- Propensity of the user to convert or purchase a new product (0.0000 to 1.0000)
    lead_conversion_probability NUMERIC(5,4),
    -- Categorical grade (e.g., 'A', 'B', 'Hot', 'Cold') for quick segmentation
    lead_grade TEXT, 

    -- 2. Churn Scoring
    -- Probability that the user will stop using the service/bank (0.0000 to 1.0000)
    churn_probability NUMERIC(5,4),
    -- Bucketized risk level for marketing automation
    churn_risk_tier TEXT CHECK (churn_risk_tier IN ('low', 'medium', 'high', 'critical')),

    -- 3. Customer Lifetime Value (CLV) Scoring
    -- Actual realized revenue/profit to date
    historical_clv NUMERIC(15,2) DEFAULT 0.00,
    -- ML-predicted future revenue generation
    predictive_clv NUMERIC(15,2),
    -- Combined or segmented CLV tier
    clv_segment TEXT,

    -- 4. Customer Experience (CX) & Engagement Scoring
    -- Overall interaction frequency/depth score (0 to 100)
    engagement_score NUMERIC(5,2),
    -- Most recent Net Promoter Score (0 to 10)
    latest_nps_score INTEGER CHECK (latest_nps_score >= 0 AND latest_nps_score <= 10),
    -- Average Customer Satisfaction Score across interactions
    average_csat NUMERIC(3,2),
    -- NLP-derived sentiment from support tickets and social mentions (-1.0 to 1.0)
    overall_sentiment_score NUMERIC(5,4),

    -- 5. Data Quality & Identity Resolution Scoring
    -- Percentage of critical profile fields filled out (0 to 100)
    profile_completeness_score NUMERIC(5,2),
    -- Confidence score of the identity stitching algorithm (0.0000 to 1.0000)
    identity_confidence_score NUMERIC(5,4),

    -- Scoring Metadata
    -- Tracks which ML model versions generated the current scores.
    -- Format: {"churn_model": "v2.1", "clv_model": "v1.4"}
    model_versions JSONB DEFAULT '{}'::JSONB,
    -- Tracks the last time the batch or streaming pipelines updated these scores.
    scores_updated_at TIMESTAMP,

    -- =========================================================================
    -- SYSTEM METADATA
    -- =========================================================================
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now(),
    status_code SMALLINT DEFAULT 1,  -- 1: active, 0: inactive, -1: delete

    -- Business rule: a profile with hashed PII is not human-readable/searchable without a
    -- persona_name stand-in. Enforced at the DB layer in addition to application code.
    CONSTRAINT chk_cdp_mp_hashed_requires_persona_name CHECK (is_hashed = FALSE OR persona_name IS NOT NULL)
);

-- Raw profiles staging
-- Landing zone for every inbound source: AppsFlyer (mobile attribution/install
-- events), MoEngage (engagement/push events), Web Tracking / GA4 (browser
-- events), and domain-specific sources like POS or Core Banking, for both the
-- retail and banking domains.
CREATE TABLE customer360.cdp_raw_profiles_stage (
    raw_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    domain TEXT NOT NULL DEFAULT 'retail' CHECK (domain IN ('retail', 'banking')),
    source_system TEXT NOT NULL,        -- 'AppsFlyer' | 'MoEngage' | 'WebTracking' | 'CoreBanking' | 'POS' | ...
    channel TEXT,                       -- 'mobile_app' | 'web' | 'pos' | 'call_center' | ...

    -- Core identity fields as reported by the source
    profile_type TEXT CHECK (profile_type IN ('individual','business','organization')) DEFAULT 'individual',
    external_customer_id TEXT,          -- AppsFlyer customer_user_id / MoEngage unique_id / core banking CIF / loyalty_id
    full_name TEXT,
    email TEXT,
    phone_number TEXT,
    national_id TEXT,                   -- banking KYC identifier (CMND/CCCD/passport)

    -- Device & marketing identity (AppsFlyer / MoEngage / Web Tracking)
    device_id TEXT,                     -- IDFV / Android ID / app instance id
    advertising_id TEXT,                -- IDFA / GAID
    platform TEXT,                      -- ios | android | web
    app_version TEXT,
    push_token TEXT,
    cookie_id TEXT,                     -- Web Tracking anonymous/browser cookie id
    ga_client_id TEXT,                  -- Google Analytics client id
    session_id TEXT,
    ip_address INET,
    user_agent TEXT,

    -- Marketing attribution (AppsFlyer install/campaign touch + Web UTM)
    media_source TEXT,
    campaign TEXT,
    utm_source TEXT,
    utm_medium TEXT,
    utm_campaign TEXT,

    event_name TEXT,                    -- e.g. install, login, page_view, purchase
    event_time TIMESTAMP WITH TIME ZONE,
    event_payload JSONB,                -- full raw source payload / extracted attributes

    status_code SMALLINT DEFAULT 1,  -- 3: processed, 2: in-progress, 1: new, 0: inactive, -1: delete
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP DEFAULT now()
);

-- Links (raw → master)
CREATE TABLE customer360.cdp_profile_links (
    link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    raw_profile_id UUID NOT NULL REFERENCES customer360.cdp_raw_profiles_stage(raw_profile_id),
    master_profile_id UUID NOT NULL REFERENCES customer360.cdp_master_profiles(master_profile_id),
    match_score NUMERIC(5,4),
    match_method TEXT,
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE(tenant_id, raw_profile_id)
);

---------------------------------------------------
-- PROFILE ATTRIBUTE METADATA REGISTRY
---------------------------------------------------

-- ============================================================================
-- cdp_profile_attributes: full attribute catalog for cdp_master_profiles
-- ============================================================================
-- One row per attribute exposed anywhere on the CDP golden record: identity /
-- demographic / retail / banking / marketing / lineage columns AND the
-- ML scoring-model outputs (Lead, Churn, CLV, Customer Experience, Data
-- Quality / Identity Resolution confidence). Also carries the
-- cdp_raw_profiles_stage matching keys (device_id, advertising_id, cookie_id,
-- external_customer_id) consumed dynamically by the Customer Identity
-- Resolution (CIR) engine (core-customer360/identity-resolution-service ->
-- identity_resolution.resolver.CustomerIdentityResolver), which only reads
-- attribute_internal_code / is_identity_resolution / status / matching_rule /
-- matching_threshold, so the extra metadata columns below are additive and
-- safe for that consumer.
-- Uses CREATE TABLE IF NOT EXISTS + ADD COLUMN IF NOT EXISTS so it stays
-- additive/idempotent for databases where a narrower cdp_profile_attributes
-- table was already created at runtime (pre-existing behavior of
-- identity-resolution-service/scripts/init_sample_data.py).
-- ============================================================================
CREATE TABLE IF NOT EXISTS customer360.cdp_profile_attributes (
    id BIGSERIAL PRIMARY KEY,

    -- Attribute identity. Matches the cdp_raw_profiles_stage column name when
    -- used as an identity-resolution matching key, otherwise matches the
    -- cdp_master_profiles column name directly.
    attribute_internal_code VARCHAR(100) UNIQUE NOT NULL,
    -- The cdp_master_profiles column this attribute is stored in / consolidated
    -- into, e.g. matching key 'device_id' consolidates into master 'device_ids'.
    master_profile_column VARCHAR(100),

    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Logical grouping for catalog browsing / admin UI.
    attribute_group VARCHAR(50) NOT NULL DEFAULT 'GENERAL' CHECK (attribute_group IN (
        'SYSTEM', 'IDENTITY', 'IDENTITY_GRAPH', 'RETAIL', 'BANKING', 'MARKETING',
        'LINEAGE', 'LEAD_SCORING', 'CHURN_SCORING', 'CLV_SCORING', 'CX_SCORING',
        'DATA_QUALITY', 'GENERAL'
    )),
    -- Physical table(s) this attribute lives on.
    source_table VARCHAR(150) NOT NULL DEFAULT 'cdp_master_profiles',
    data_type VARCHAR(50) NOT NULL DEFAULT 'TEXT',
    domain_scope VARCHAR(20) NOT NULL DEFAULT 'all' CHECK (domain_scope IN ('all', 'retail', 'banking')),
    is_pii BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',

    -- ------------------------------------------------------------------
    -- Customer Identity Resolution (CIR) matching-rule metadata, consumed
    -- dynamically by identity_resolution.resolver.CustomerIdentityResolver.
    -- ------------------------------------------------------------------
    is_identity_resolution BOOLEAN NOT NULL DEFAULT FALSE,
    matching_rule VARCHAR(50) CHECK (matching_rule IN ('exact', 'fuzzy_trgm', 'fuzzy_dmetaphone', 'none')),
    matching_threshold NUMERIC(5, 4),
    consolidation_rule VARCHAR(50),

    -- ------------------------------------------------------------------
    -- ML / scoring-model metadata: Lead, Churn, CLV, Customer Experience (CX)
    -- and Data Quality / Identity Resolution confidence scoring models.
    -- ------------------------------------------------------------------
    is_scoring_model BOOLEAN NOT NULL DEFAULT FALSE,
    scoring_model_name VARCHAR(100),
    scoring_model_version VARCHAR(20),
    value_type VARCHAR(50) CHECK (value_type IS NULL OR value_type IN (
        'probability', 'score', 'tier', 'currency', 'percentage', 'sentiment',
        'count', 'label', 'metadata', 'identifier', 'timestamp'
    )),
    value_min NUMERIC,
    value_max NUMERIC,
    -- How often this attribute/score gets (re)computed: 'realtime' | 'hourly' |
    -- 'daily' | 'weekly' | 'batch' | 'event_driven'.
    refresh_frequency VARCHAR(50),

    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Upgrade path for any pre-existing (narrower) cdp_profile_attributes table.
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS master_profile_column VARCHAR(100);
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS attribute_group VARCHAR(50) NOT NULL DEFAULT 'GENERAL';
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS source_table VARCHAR(150) NOT NULL DEFAULT 'cdp_master_profiles';
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS domain_scope VARCHAR(20) NOT NULL DEFAULT 'all';
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS is_pii BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS is_scoring_model BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS scoring_model_name VARCHAR(100);
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS scoring_model_version VARCHAR(20);
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS value_type VARCHAR(50);
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS value_min NUMERIC;
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS value_max NUMERIC;
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS refresh_frequency VARCHAR(50);
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0;
ALTER TABLE customer360.cdp_profile_attributes ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT now();

---------------------------------------------------
-- RELATIONS & EVENTS
---------------------------------------------------

-- Relation Types dictionary
CREATE TABLE customer360.relation_types (
    relation_type_id SERIAL PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,   -- e.g., 'friend', 'colleague', 'family', 'customer-contact'
    description TEXT
);

-- Profile Relations
CREATE TABLE customer360.cdp_relations (
    relation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    source_master_id UUID NOT NULL REFERENCES customer360.cdp_master_profiles(master_profile_id),
    target_master_id UUID NOT NULL REFERENCES customer360.cdp_master_profiles(master_profile_id),
    relation_type_id INT NOT NULL REFERENCES customer360.relation_types(relation_type_id),
    created_at TIMESTAMP DEFAULT now(),
    UNIQUE (tenant_id, source_master_id, target_master_id, relation_type_id)
);

-- Customer Contacts (interactions)
CREATE TABLE customer360.customer_contacts (
    contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    master_profile_id UUID NOT NULL REFERENCES customer360.cdp_master_profiles(master_profile_id),
    contact_type TEXT,
    contact_channel TEXT,
    contact_content TEXT,
    contact_date TIMESTAMP DEFAULT now()
);

-- Purchases
CREATE TABLE customer360.purchases (
    purchase_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    master_profile_id UUID NOT NULL REFERENCES customer360.cdp_master_profiles(master_profile_id),
    product_id UUID,
    product_name TEXT,
    amount NUMERIC(12,2),
    currency TEXT DEFAULT 'USD',
    purchase_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

---------------------------------------------------
-- GRAPH EDGES (Partitioned by Relation)
---------------------------------------------------

-- Parent
CREATE TABLE customer360.graph_edges (
    edge_id BIGSERIAL NOT NULL,
    from_id UUID NOT NULL,
    to_id UUID NOT NULL,
    from_type TEXT NOT NULL,
    to_type TEXT NOT NULL,
    relation TEXT NOT NULL,
    description TEXT,
    keywords TEXT[],
    lang TEXT DEFAULT 'en',
    embedding vector(1536),
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (edge_id, relation)
) PARTITION BY LIST (relation);

-- Partitions for known relations
CREATE TABLE customer360.graph_edges_belongs_to
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('belongs_to');

CREATE TABLE customer360.graph_edges_comes_from
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('comes_from');

CREATE TABLE customer360.graph_edges_converted
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('converted');

CREATE TABLE customer360.graph_edges_follows
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('follows');

CREATE TABLE customer360.graph_edges_is_part_of
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('is_part_of');

CREATE TABLE customer360.graph_edges_is_active_as
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('is_active_as');

CREATE TABLE customer360.graph_edges_is_connected_to
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('is_connected_to');

CREATE TABLE customer360.graph_edges_is_from
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('is_from');

CREATE TABLE customer360.graph_edges_created_by
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('created_by');

CREATE TABLE customer360.graph_edges_is_driven_by
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('is_driven_by');

CREATE TABLE customer360.graph_edges_has_role
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('has_role');

CREATE TABLE customer360.graph_edges_has
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('has');

CREATE TABLE customer360.graph_edges_is_for_the
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('is_for_the');

CREATE TABLE customer360.graph_edges_belongs_to_industry
    PARTITION OF customer360.graph_edges
    FOR VALUES IN ('belongs_to_industry');

-- Catch-all
CREATE TABLE customer360.graph_edges_other
    PARTITION OF customer360.graph_edges DEFAULT;

---------------------------------------------------
-- INDEXES
---------------------------------------------------

-- =========================================================================
-- RECOMMENDED INDICES FOR LEO CDP MASTER PROFILES
-- =========================================================================

-- -------------------------------------------------------------------------
-- 1. ENTITY & IDENTITY INDEXES (B-TREE)
-- Upgraded to UNIQUE per tenant_id to guarantee that master profiles 
-- remain true "golden records" without duplicates in a single workspace.
-- -------------------------------------------------------------------------

-- Email is unique per workspace. Ignored if NULL (e.g., mobile-only users).
CREATE UNIQUE INDEX ux_cdp_mp_tenant_email 
ON customer360.cdp_master_profiles (tenant_id, email) 
WHERE email IS NOT NULL;

-- Phone is unique per workspace. Ignored if NULL (e.g., web-only users).
CREATE UNIQUE INDEX ux_cdp_mp_tenant_phone 
ON customer360.cdp_master_profiles (tenant_id, phone_number) 
WHERE phone_number IS NOT NULL;

-- Core Banking & Retail Identifiers (Must be unique per tenant)
CREATE UNIQUE INDEX ux_cdp_mp_tenant_national_id 
ON customer360.cdp_master_profiles (tenant_id, national_id) 
WHERE national_id IS NOT NULL;

CREATE UNIQUE INDEX ux_cdp_mp_tenant_cif_number 
ON customer360.cdp_master_profiles (tenant_id, cif_number) 
WHERE cif_number IS NOT NULL;

CREATE UNIQUE INDEX ux_cdp_mp_tenant_loyalty_id 
ON customer360.cdp_master_profiles (tenant_id, loyalty_id) 
WHERE loyalty_id IS NOT NULL;


-- -------------------------------------------------------------------------
-- 2. ML, SCORING & SEGMENTATION INDEXES (B-TREE)
-- Improved by leading with tenant_id. Since segmentation queries always 
-- happen within a specific tenant, this massively speeds up campaign lookups.
-- -------------------------------------------------------------------------

-- Fast retrieval for churn prevention campaigns (Partial index saves space)
CREATE INDEX idx_cdp_mp_churn_tier 
ON customer360.cdp_master_profiles (tenant_id, churn_risk_tier) 
WHERE churn_risk_tier IN ('high', 'critical');

-- Fast retrieval for high-value customer targeting (Whales)
CREATE INDEX idx_cdp_mp_pred_clv 
ON customer360.cdp_master_profiles (tenant_id, predictive_clv DESC NULLS LAST);

-- Fast routing of high-probability leads to sales/CRM
CREATE INDEX idx_cdp_mp_lead_prob 
ON customer360.cdp_master_profiles (tenant_id, lead_conversion_probability DESC NULLS LAST);

-- Analytics lookup for profiles needing data enrichment
CREATE INDEX idx_cdp_mp_data_quality 
ON customer360.cdp_master_profiles (tenant_id, profile_completeness_score, identity_confidence_score);


-- -------------------------------------------------------------------------
-- 3. CROSS-CHANNEL IDENTITY GRAPH INDEXES (GIN)
-- Used for fast querying inside JSON objects and TEXT arrays.
-- Deduplicated external_ids and standardized names.
-- 
-- Note: If you frequently query these alongside tenant_id, consider enabling 
-- the 'btree_gin' PostgreSQL extension to allow (tenant_id, json_column) 
-- composite GIN indexes in the future.
-- -------------------------------------------------------------------------

-- Deterministic external IDs (e.g., {"appsflyer_id": "...", "ga_client_id": "..."})
CREATE INDEX idx_cdp_mp_external_ids ON customer360.cdp_master_profiles USING GIN (external_ids);

-- Secondary contacts
CREATE INDEX idx_cdp_mp_sec_emails ON customer360.cdp_master_profiles USING GIN (secondary_emails);
CREATE INDEX idx_cdp_mp_sec_phones ON customer360.cdp_master_profiles USING GIN (secondary_phones);

-- Device & Ad Graph (Arrays)
CREATE INDEX idx_cdp_mp_device_ids ON customer360.cdp_master_profiles USING GIN (device_ids);
CREATE INDEX idx_cdp_mp_advertising_ids ON customer360.cdp_master_profiles USING GIN (advertising_ids);
CREATE INDEX idx_cdp_mp_cookie_ids ON customer360.cdp_master_profiles USING GIN (cookie_ids);

-- Raw staging indexes: identity fields used for matching, plus the
-- processing-queue lookup (tenant_id, status_code).
CREATE INDEX idx_raw_profiles_stage_tenant_status ON customer360.cdp_raw_profiles_stage(tenant_id, status_code);
CREATE INDEX idx_raw_profiles_stage_email ON customer360.cdp_raw_profiles_stage(email) WHERE email IS NOT NULL;
CREATE INDEX idx_raw_profiles_stage_phone ON customer360.cdp_raw_profiles_stage(phone_number) WHERE phone_number IS NOT NULL;
CREATE INDEX idx_raw_profiles_stage_external_customer_id ON customer360.cdp_raw_profiles_stage(external_customer_id) WHERE external_customer_id IS NOT NULL;
CREATE INDEX idx_raw_profiles_stage_device_id ON customer360.cdp_raw_profiles_stage(device_id) WHERE device_id IS NOT NULL;
CREATE INDEX idx_raw_profiles_stage_advertising_id ON customer360.cdp_raw_profiles_stage(advertising_id) WHERE advertising_id IS NOT NULL;
CREATE INDEX idx_raw_profiles_stage_cookie_id ON customer360.cdp_raw_profiles_stage(cookie_id) WHERE cookie_id IS NOT NULL;
CREATE INDEX idx_raw_profiles_stage_national_id ON customer360.cdp_raw_profiles_stage(national_id) WHERE national_id IS NOT NULL;
CREATE INDEX idx_purchases_date ON customer360.purchases(purchase_date);
CREATE INDEX idx_contacts_date ON customer360.customer_contacts(contact_date);

-- Profile attribute metadata registry: catalog browsing by group, fast
-- lookup of active CIR matching rules, and lookup of attributes by scoring model.
CREATE INDEX IF NOT EXISTS idx_cdp_pa_group ON customer360.cdp_profile_attributes (attribute_group);
CREATE INDEX IF NOT EXISTS idx_cdp_pa_identity_resolution ON customer360.cdp_profile_attributes (attribute_internal_code) WHERE is_identity_resolution = TRUE AND status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_cdp_pa_scoring_model ON customer360.cdp_profile_attributes (scoring_model_name) WHERE is_scoring_model = TRUE;

-- Graph edges indexes
CREATE INDEX ON customer360.graph_edges_belongs_to (from_id, to_id);
CREATE INDEX ON customer360.graph_edges_comes_from (from_id);
CREATE INDEX ON customer360.graph_edges_converted (from_id);
CREATE INDEX ON customer360.graph_edges_follows USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX ON customer360.graph_edges_is_driven_by (created_at);
CREATE INDEX ON customer360.graph_edges_belongs_to_industry (created_at);

---------------------------------------------------
-- PROFILE ATTRIBUTE METADATA REGISTRY: SEED DATA
---------------------------------------------------

-- Full attribute catalog for cdp_master_profiles (every column, grouped) plus
-- the cdp_raw_profiles_stage matching keys used by identity-resolution-service.
-- Idempotent: safe to re-run (ON CONFLICT upserts by attribute_internal_code).
INSERT INTO customer360.cdp_profile_attributes (
    attribute_internal_code, master_profile_column, name, description,
    attribute_group, source_table, data_type, domain_scope, is_pii, status,
    is_identity_resolution, matching_rule, matching_threshold, consolidation_rule,
    is_scoring_model, scoring_model_name, scoring_model_version, value_type,
    value_min, value_max, refresh_frequency, display_order
) VALUES
-- SYSTEM
('master_profile_id', 'master_profile_id', 'Master Profile ID', 'Primary key of the golden, resolved customer record.', 'SYSTEM', 'cdp_master_profiles', 'UUID', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 10),
('tenant_id', 'tenant_id', 'Tenant ID', 'Workspace/tenant scope used for multi-tenant data isolation.', 'SYSTEM', 'cdp_master_profiles', 'UUID', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 20),
('domain', 'domain', 'Business Domain', 'retail or banking; drives domain-specific UI and activation logic.', 'SYSTEM', 'cdp_master_profiles', 'TEXT', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 30),
('created_at', 'created_at', 'Profile Created At', 'Timestamp the master profile was first created.', 'SYSTEM', 'cdp_master_profiles', 'TIMESTAMP', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'timestamp', NULL, NULL, NULL, 40),
('updated_at', 'updated_at', 'Profile Updated At', 'Timestamp of the most recent update to this profile.', 'SYSTEM', 'cdp_master_profiles', 'TIMESTAMP', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'timestamp', NULL, NULL, NULL, 50),

-- IDENTITY (demographics + core/secondary contact info)
('full_name', 'full_name', 'Full Name', 'Customer full display name; identity-resolution matching key (exact, SHA-256 hashed).', 'IDENTITY', 'cdp_master_profiles, cdp_raw_profiles_stage', 'TEXT', 'all', TRUE, 'ACTIVE', TRUE, 'exact', NULL, 'most_recent', FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 60),
('first_name', 'first_name', 'First Name', 'Given name.', 'IDENTITY', 'cdp_master_profiles', 'TEXT', 'all', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 70),
('last_name', 'last_name', 'Last Name', 'Family name.', 'IDENTITY', 'cdp_master_profiles', 'TEXT', 'all', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 80),
('is_hashed', 'is_hashed', 'PII Is Hashed', 'True if full_name/email/phone_number/national_id are SHA-256 hashed (hashed-match ingestion). When TRUE, persona_name is required.', 'IDENTITY', 'cdp_master_profiles', 'BOOLEAN', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 85),
('email', 'email', 'Email Address', 'Primary email; identity-resolution matching key (exact, SHA-256 hashed).', 'IDENTITY', 'cdp_master_profiles, cdp_raw_profiles_stage', 'TEXT', 'all', TRUE, 'ACTIVE', TRUE, 'exact', NULL, 'non_null', FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 90),
('phone_number', 'phone_number', 'Phone Number', 'Primary phone; identity-resolution matching key (exact, SHA-256 hashed).', 'IDENTITY', 'cdp_master_profiles, cdp_raw_profiles_stage', 'TEXT', 'all', TRUE, 'ACTIVE', TRUE, 'exact', NULL, 'non_null', FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 100),
('secondary_emails', 'secondary_emails', 'Secondary Emails', 'Additional emails, e.g. [{"email":"work@abc.com","label":"work"}].', 'IDENTITY', 'cdp_master_profiles', 'JSONB', 'all', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'metadata', NULL, NULL, NULL, 110),
('secondary_phones', 'secondary_phones', 'Secondary Phones', 'Additional phone numbers, e.g. [{"phone":"+84901234567","label":"home"}].', 'IDENTITY', 'cdp_master_profiles', 'JSONB', 'all', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'metadata', NULL, NULL, NULL, 120),
('date_of_birth', 'date_of_birth', 'Date of Birth', 'Customer date of birth.', 'IDENTITY', 'cdp_master_profiles', 'DATE', 'all', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, NULL, NULL, NULL, NULL, 130),
('gender', 'gender', 'Gender', 'male, female, or other.', 'IDENTITY', 'cdp_master_profiles', 'TEXT', 'all', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 140),
('address', 'address', 'Address', 'Flexible address document, e.g. {"street":"123 Le Loi","city":"Ho Chi Minh","country":"VN"}.', 'IDENTITY', 'cdp_master_profiles', 'JSONB', 'all', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'metadata', NULL, NULL, NULL, 150),

-- IDENTITY_GRAPH (cross-channel device/ad/cookie/external identifiers)
('external_ids', 'external_ids', 'External System IDs', 'Map of source_system to that source external customer id (deterministic matching).', 'IDENTITY_GRAPH', 'cdp_master_profiles', 'JSONB', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'metadata', NULL, NULL, NULL, 160),
('external_customer_id', 'external_ids', 'External Customer ID (raw)', 'Per-source customer id on cdp_raw_profiles_stage (AppsFlyer customer_user_id / core banking CIF / loyalty_id); identity-resolution matching key, consolidated into external_ids.', 'IDENTITY_GRAPH', 'cdp_raw_profiles_stage', 'TEXT', 'all', FALSE, 'ACTIVE', TRUE, 'exact', NULL, 'non_null', FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 170),
('device_ids', 'device_ids', 'Device IDs', 'Consolidated array of device identifiers (IDFV/Android ID/app instance id).', 'IDENTITY_GRAPH', 'cdp_master_profiles', 'ARRAY', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 180),
('device_id', 'device_ids', 'Device ID (raw)', 'Raw per-event device id on cdp_raw_profiles_stage; identity-resolution matching key, consolidated into device_ids.', 'IDENTITY_GRAPH', 'cdp_raw_profiles_stage', 'TEXT', 'all', FALSE, 'ACTIVE', TRUE, 'exact', NULL, 'non_null', FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 190),
('advertising_ids', 'advertising_ids', 'Advertising IDs', 'Consolidated array of mobile advertising identifiers (IDFA/GAID) for retargeting.', 'IDENTITY_GRAPH', 'cdp_master_profiles', 'ARRAY', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 200),
('advertising_id', 'advertising_ids', 'Advertising ID (raw)', 'Raw per-event advertising id on cdp_raw_profiles_stage; identity-resolution matching key, consolidated into advertising_ids.', 'IDENTITY_GRAPH', 'cdp_raw_profiles_stage', 'TEXT', 'all', FALSE, 'ACTIVE', TRUE, 'exact', NULL, 'non_null', FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 210),
('cookie_ids', 'cookie_ids', 'Cookie IDs', 'Consolidated array of anonymous browser cookies for web session stitching.', 'IDENTITY_GRAPH', 'cdp_master_profiles', 'ARRAY', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 220),
('cookie_id', 'cookie_ids', 'Cookie ID (raw)', 'Raw per-event web cookie id on cdp_raw_profiles_stage; identity-resolution matching key, consolidated into cookie_ids.', 'IDENTITY_GRAPH', 'cdp_raw_profiles_stage', 'TEXT', 'all', FALSE, 'ACTIVE', TRUE, 'exact', NULL, 'non_null', FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 230),
('push_tokens', 'push_tokens', 'Push Notification Tokens', 'Stored push tokens, e.g. {"fcm":"token","apns":"token"}.', 'IDENTITY_GRAPH', 'cdp_master_profiles', 'JSONB', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'metadata', NULL, NULL, NULL, 240),

-- RETAIL
('loyalty_id', 'loyalty_id', 'Loyalty ID', 'Retail loyalty program membership identifier.', 'RETAIL', 'cdp_master_profiles', 'TEXT', 'retail', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 250),
('membership_tier', 'membership_tier', 'Membership Tier', 'Loyalty program tier (e.g. Silver/Gold/Platinum).', 'RETAIL', 'cdp_master_profiles', 'TEXT', 'retail', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'tier', NULL, NULL, NULL, 260),
('preferred_store_code', 'preferred_store_code', 'Preferred Store Code', 'Physical store the customer shops at most often.', 'RETAIL', 'cdp_master_profiles', 'TEXT', 'retail', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 270),

-- BANKING
('national_id', 'national_id', 'National ID / KYC ID', 'CMND/CCCD/passport number; identity-resolution matching key (exact, SHA-256 hashed).', 'BANKING', 'cdp_master_profiles, cdp_raw_profiles_stage', 'TEXT', 'banking', TRUE, 'ACTIVE', TRUE, 'exact', NULL, 'non_null', FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 280),
('cif_number', 'cif_number', 'Core Banking CIF Number', 'Customer Information File number; the golden record id in legacy core banking.', 'BANKING', 'cdp_master_profiles', 'TEXT', 'banking', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 290),
('account_numbers', 'account_numbers', 'Account Numbers', 'Array of active bank account numbers associated with this CIF.', 'BANKING', 'cdp_master_profiles', 'ARRAY', 'banking', TRUE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 300),
('kyc_status', 'kyc_status', 'KYC Status', 'unverified, pending, verified, or rejected.', 'BANKING', 'cdp_master_profiles', 'TEXT', 'banking', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 310),
('risk_segment', 'risk_segment', 'Risk Segment', 'AML/credit risk categorization.', 'BANKING', 'cdp_master_profiles', 'TEXT', 'banking', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 320),

-- MARKETING
('acquisition_source', 'acquisition_source', 'Acquisition Source', 'First-touch channel attribution (e.g. organic_search, paid_social).', 'MARKETING', 'cdp_master_profiles', 'TEXT', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 330),
('acquisition_campaign', 'acquisition_campaign', 'Acquisition Campaign', 'First-touch campaign attribution.', 'MARKETING', 'cdp_master_profiles', 'TEXT', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 340),
('persona_name', 'persona_name', 'Persona Name', 'Human-readable, non-PII label for segmentation/marketing and semantic search (e.g. "Gen Z Shopper"). Required whenever is_hashed = TRUE; auto-generated by identity-resolution-service when real PII is hashed.', 'MARKETING', 'cdp_master_profiles', 'TEXT', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 345),
('persona_embedding', 'persona_embedding', 'Persona Embedding', 'LLM-generated embedding used for semantic search / lookalike modeling.', 'MARKETING', 'cdp_master_profiles', 'VECTOR', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'metadata', NULL, NULL, NULL, 350),
('segmentation_tags', 'segmentation_tags', 'Segmentation Tags', 'Computed labels for fast Audience Builder queries (e.g. gen_z, frequent_buyer).', 'MARKETING', 'cdp_master_profiles', 'ARRAY', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'label', NULL, NULL, NULL, 360),
('attributes', 'attributes', 'Custom Attributes', 'Schemaless payload of dynamically extracted traits (e.g. occupation, income_segment).', 'MARKETING', 'cdp_master_profiles', 'JSONB', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'metadata', NULL, NULL, NULL, 370),

-- LINEAGE
('source_systems', 'source_systems', 'Source Systems', 'All external systems that have contributed data to this profile.', 'LINEAGE', 'cdp_master_profiles', 'ARRAY', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 380),
('first_seen_raw_profile_id', 'first_seen_raw_profile_id', 'First Seen Raw Profile ID', 'Lineage pointer back to the raw_profile_id that initiated this profile.', 'LINEAGE', 'cdp_master_profiles', 'UUID', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'identifier', NULL, NULL, NULL, 390),

-- LEAD & CONVERSION SCORING
('lead_conversion_probability', 'lead_conversion_probability', 'Lead Conversion Probability', 'ML-predicted probability the profile converts or purchases a new product.', 'LEAD_SCORING', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'lead_scoring_model', 'v1', 'probability', 0, 1, 'daily', 400),
('lead_grade', 'lead_grade', 'Lead Grade', 'Categorical grade (e.g. A/B, Hot/Cold) derived from lead_conversion_probability for quick segmentation.', 'LEAD_SCORING', 'cdp_master_profiles', 'TEXT', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'lead_scoring_model', 'v1', 'tier', NULL, NULL, 'daily', 410),

-- CHURN SCORING
('churn_probability', 'churn_probability', 'Churn Probability', 'ML-predicted probability the user stops using the service/bank.', 'CHURN_SCORING', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'churn_scoring_model', 'v1', 'probability', 0, 1, 'daily', 420),
('churn_risk_tier', 'churn_risk_tier', 'Churn Risk Tier', 'Bucketized churn risk (low/medium/high/critical) for marketing automation.', 'CHURN_SCORING', 'cdp_master_profiles', 'TEXT', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'churn_scoring_model', 'v1', 'tier', NULL, NULL, 'daily', 430),

-- CUSTOMER LIFETIME VALUE (CLV) SCORING
('historical_clv', 'historical_clv', 'Historical CLV', 'Actual realized revenue/profit to date.', 'CLV_SCORING', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'currency', 0, NULL, 'weekly', 440),
('predictive_clv', 'predictive_clv', 'Predictive CLV', 'ML-predicted future revenue generation.', 'CLV_SCORING', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'clv_scoring_model', 'v1', 'currency', 0, NULL, 'weekly', 450),
('clv_segment', 'clv_segment', 'CLV Segment', 'Combined or segmented CLV tier (e.g. high/medium/low value).', 'CLV_SCORING', 'cdp_master_profiles', 'TEXT', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'clv_scoring_model', 'v1', 'tier', NULL, NULL, 'weekly', 460),

-- CUSTOMER EXPERIENCE (CX) & ENGAGEMENT SCORING
('engagement_score', 'engagement_score', 'Engagement Score', 'Overall interaction frequency/depth score.', 'CX_SCORING', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'cx_scoring_model', 'v1', 'score', 0, 100, 'daily', 470),
('latest_nps_score', 'latest_nps_score', 'Latest NPS Score', 'Most recent Net Promoter Score.', 'CX_SCORING', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'cx_scoring_model', 'v1', 'score', 0, 10, 'event_driven', 480),
('average_csat', 'average_csat', 'Average CSAT', 'Average Customer Satisfaction Score across interactions.', 'CX_SCORING', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'cx_scoring_model', 'v1', 'score', 0, 5, 'daily', 490),
('overall_sentiment_score', 'overall_sentiment_score', 'Overall Sentiment Score', 'NLP-derived sentiment from support tickets and social mentions.', 'CX_SCORING', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'cx_scoring_model', 'v1', 'sentiment', -1, 1, 'daily', 500),

-- DATA QUALITY & IDENTITY RESOLUTION SCORING
('profile_completeness_score', 'profile_completeness_score', 'Profile Completeness Score', 'Percentage of critical profile fields filled out.', 'DATA_QUALITY', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'data_quality_model', 'v1', 'percentage', 0, 100, 'daily', 510),
('identity_confidence_score', 'identity_confidence_score', 'Identity Confidence Score', 'Confidence score of the identity-stitching (CIR) algorithm.', 'DATA_QUALITY', 'cdp_master_profiles', 'NUMERIC', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, TRUE, 'identity_resolution_scoring_model', 'v1', 'probability', 0, 1, 'realtime', 520),
('model_versions', 'model_versions', 'Model Versions', 'Tracks which ML model versions generated the current scores, e.g. {"churn_model":"v2.1","clv_model":"v1.4"}.', 'DATA_QUALITY', 'cdp_master_profiles', 'JSONB', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'metadata', NULL, NULL, NULL, 530),
('scores_updated_at', 'scores_updated_at', 'Scores Updated At', 'Last time the batch or streaming pipelines updated the scoring fields.', 'DATA_QUALITY', 'cdp_master_profiles', 'TIMESTAMP', 'all', FALSE, 'ACTIVE', FALSE, NULL, NULL, NULL, FALSE, NULL, NULL, 'timestamp', NULL, NULL, NULL, 540)

ON CONFLICT (attribute_internal_code) DO UPDATE SET
    master_profile_column = EXCLUDED.master_profile_column,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    attribute_group = EXCLUDED.attribute_group,
    source_table = EXCLUDED.source_table,
    data_type = EXCLUDED.data_type,
    domain_scope = EXCLUDED.domain_scope,
    is_pii = EXCLUDED.is_pii,
    status = EXCLUDED.status,
    is_identity_resolution = EXCLUDED.is_identity_resolution,
    matching_rule = EXCLUDED.matching_rule,
    matching_threshold = EXCLUDED.matching_threshold,
    consolidation_rule = EXCLUDED.consolidation_rule,
    is_scoring_model = EXCLUDED.is_scoring_model,
    scoring_model_name = EXCLUDED.scoring_model_name,
    scoring_model_version = EXCLUDED.scoring_model_version,
    value_type = EXCLUDED.value_type,
    value_min = EXCLUDED.value_min,
    value_max = EXCLUDED.value_max,
    refresh_frequency = EXCLUDED.refresh_frequency,
    display_order = EXCLUDED.display_order,
    updated_at = now();




