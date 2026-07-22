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

-- Golden customer profile
-- Consolidated ("resolved") identity across sources (AppsFlyer, MoEngage, Web
-- Tracking / GA4, POS, Core Banking, ...) for both the retail and banking domains.
CREATE TABLE customer360.cdp_master_profiles (
    master_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    domain TEXT NOT NULL DEFAULT 'retail' CHECK (domain IN ('retail', 'banking')),

    -- Core identity
    full_name TEXT,
    first_name TEXT,
    last_name TEXT,
    email TEXT,
    phone_number TEXT,
    date_of_birth DATE,
    gender TEXT CHECK (gender IN ('male','female','other')),
    address JSONB,

    -- Cross-channel identity graph (resolved from cdp_raw_profiles_stage).
    -- external_ids maps a source_system to its own customer identifier, e.g.
    -- {"appsflyer_id": "...", "moengage_id": "...", "ga_client_id": "...", "core_banking_cif": "..."}.
    external_ids JSONB DEFAULT '{}'::JSONB,
    device_ids TEXT[] DEFAULT ARRAY[]::TEXT[],       -- AppsFlyer/MoEngage device ids (IDFV/Android ID/app instance id)
    advertising_ids TEXT[] DEFAULT ARRAY[]::TEXT[],  -- AppsFlyer IDFA/GAID
    cookie_ids TEXT[] DEFAULT ARRAY[]::TEXT[],       -- Web Tracking anonymous/browser cookie ids
    push_tokens JSONB DEFAULT '{}'::JSONB,           -- MoEngage/app push tokens, e.g. {"fcm": "...", "apns": "..."}

    -- Retail domain attributes
    loyalty_id TEXT,
    membership_tier TEXT,
    preferred_store_code TEXT,

    -- Banking domain attributes (KYC / core banking)
    national_id TEXT,                                -- CMND/CCCD/passport number
    cif_number TEXT,                                 -- Core Banking Customer Information File number
    account_numbers TEXT[] DEFAULT ARRAY[]::TEXT[],
    kyc_status TEXT CHECK (kyc_status IN ('unverified','pending','verified','rejected')),
    risk_segment TEXT,

    -- Marketing / engagement (first-touch attribution, e.g. from AppsFlyer)
    acquisition_source TEXT,
    acquisition_campaign TEXT,
    persona_embedding vector(768),
    segmentation_tags TEXT[],
    attributes JSONB DEFAULT '{}'::JSONB,            -- free-form extracted traits (occupation, income_segment, preferred_features, ...)
    source_systems TEXT[] DEFAULT ARRAY[]::TEXT[],   -- every source_system that has contributed to this profile
    first_seen_raw_profile_id UUID,                  -- raw_profile_id that first created this master profile

    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
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

-- Entity indexes
-- Email/phone are only unique per tenant (multi-tenant schema), and both are
-- optional (mobile/app-only or banking-only profiles may have neither).
CREATE UNIQUE INDEX ux_master_profiles_tenant_email ON customer360.cdp_master_profiles(tenant_id, email) WHERE email IS NOT NULL;
CREATE INDEX idx_master_profiles_tenant_phone ON customer360.cdp_master_profiles(tenant_id, phone_number) WHERE phone_number IS NOT NULL;
CREATE INDEX idx_master_profiles_national_id ON customer360.cdp_master_profiles(national_id) WHERE national_id IS NOT NULL;
CREATE INDEX idx_master_profiles_cif_number ON customer360.cdp_master_profiles(cif_number) WHERE cif_number IS NOT NULL;
CREATE INDEX idx_master_profiles_loyalty_id ON customer360.cdp_master_profiles(loyalty_id) WHERE loyalty_id IS NOT NULL;
CREATE INDEX idx_master_profiles_device_ids ON customer360.cdp_master_profiles USING GIN (device_ids);
CREATE INDEX idx_master_profiles_advertising_ids ON customer360.cdp_master_profiles USING GIN (advertising_ids);
CREATE INDEX idx_master_profiles_cookie_ids ON customer360.cdp_master_profiles USING GIN (cookie_ids);
CREATE INDEX idx_master_profiles_external_ids ON customer360.cdp_master_profiles USING GIN (external_ids);

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

-- Graph edges indexes
CREATE INDEX ON customer360.graph_edges_belongs_to (from_id, to_id);
CREATE INDEX ON customer360.graph_edges_comes_from (from_id);
CREATE INDEX ON customer360.graph_edges_converted (from_id);
CREATE INDEX ON customer360.graph_edges_follows USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX ON customer360.graph_edges_is_driven_by (created_at);
CREATE INDEX ON customer360.graph_edges_belongs_to_industry (created_at);



