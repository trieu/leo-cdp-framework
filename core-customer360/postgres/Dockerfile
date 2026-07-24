# PostgreSQL 16 with PostGIS + pgvector + contrib extensions (uuid-ossp,
# pgcrypto, pg_trgm, fuzzystrmatch) needed by core-customer360/database-schema.sql
# and by identity-resolution-service's fuzzy-matching rules.
#
# Build context is the core-customer360/ directory (see docker-compose.yml)
# so this Dockerfile can COPY the top-level database-schema.sql file.
FROM postgis/postgis:16-3.5

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-16-pgvector \
    && rm -rf /var/lib/apt/lists/*

# Scripts in /docker-entrypoint-initdb.d/ run once, in filename order, ONLY
# when the data directory is empty (first-ever container start). See:
# https://hub.docker.com/_/postgres -> "Initialization scripts".
COPY postgres/init/00-extensions.sql /docker-entrypoint-initdb.d/00-extensions.sql
COPY database-schema.sql /docker-entrypoint-initdb.d/01-schema.sql

HEALTHCHECK --interval=10s --timeout=5s --start-period=20s --retries=5 \
    CMD pg_isready -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" || exit 1
