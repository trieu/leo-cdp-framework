-- Extensions required by database-schema.sql (uuid-ossp, pgcrypto, vector,
-- postgis) plus the fuzzy-matching extensions used by identity-resolution-
-- service's matching rules (pg_trgm, fuzzystrmatch). Created up front so a
-- fresh production database is fully usable even if the dev-only demo-data
-- seeder (which also idempotently creates pg_trgm/fuzzystrmatch) never runs.
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;
