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
CREATE TABLE customer360.cdp_master_profiles (
    master_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    full_name TEXT,
    email TEXT UNIQUE,
    phone_number TEXT,
    date_of_birth DATE,
    gender TEXT CHECK (gender IN ('male','female','other')),
    address JSONB,
    persona_embedding vector(768),
    segmentation_tags TEXT[],
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

-- Raw profiles staging
CREATE TABLE customer360.cdp_raw_profiles_stage (
    raw_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    source_system TEXT NOT NULL,
    full_name TEXT,
    email TEXT,
    phone_number TEXT,
    event_payload JSONB,
    status_code SMALLINT DEFAULT 1,  -- 3: processed, 2: in-progress, 1: new, 0: inactive, -1: delete
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
CREATE INDEX idx_master_profiles_email ON customer360.cdp_master_profiles(email);
CREATE INDEX idx_master_profiles_phone ON customer360.cdp_master_profiles(phone_number);
CREATE INDEX idx_purchases_date ON customer360.purchases(purchase_date);
CREATE INDEX idx_contacts_date ON customer360.customer_contacts(contact_date);

-- Graph edges indexes
CREATE INDEX ON customer360.graph_edges_belongs_to (from_id, to_id);
CREATE INDEX ON customer360.graph_edges_comes_from (from_id);
CREATE INDEX ON customer360.graph_edges_converted (from_id);
CREATE INDEX ON customer360.graph_edges_follows USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX ON customer360.graph_edges_is_driven_by (created_at);
CREATE INDEX ON customer360.graph_edges_belongs_to_industry (created_at);



