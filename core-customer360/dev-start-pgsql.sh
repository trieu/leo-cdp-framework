#!/bin/bash

# --- Config for local DEV only ---
CONTAINER_NAME="pgsql16_vector"
POSTGRES_USER="postgres"
POSTGRES_PASSWORD="password"
DEFAULT_DB="postgres"
TARGET_DB="customer360"
HOST_PORT=5432
DATA_VOLUME="pgdata_vector"
SCHEMA_VERSION=1   # Increment this whenever you change schema.sql
SCHEMA_FILE="database-schema.sql"

# --- Usage ---
# ./dev-start-pgsql.sh            Start (or create) the container/database and
#                                 apply database-schema.sql if not already applied.
# ./dev-start-pgsql.sh reset      DESTRUCTIVE: drop and recreate the '${TARGET_DB}'
#                                 database from scratch, then re-apply the full
#                                 database-schema.sql. Use this for local dev/unit
#                                 testing whenever the schema changes, instead of
#                                 relying on the SCHEMA_VERSION upgrade path.
# ./dev-start-pgsql.sh reset -y   Same as above but skips the confirmation prompt
#                                 (for CI / automated test runs).
ACTION="${1:-up}"
SKIP_CONFIRM="false"
if [[ "${2:-}" == "-y" || "${2:-}" == "--yes" ]]; then
  SKIP_CONFIRM="true"
fi

if [[ "$ACTION" == "-h" || "$ACTION" == "--help" ]]; then
  echo "Usage: $0 [reset [-y|--yes]]"
  echo "  (no args)   Start/create the container+database and apply the schema if needed."
  echo "  reset       DESTRUCTIVE: drop + recreate '${TARGET_DB}' and re-apply the full schema."
  echo "  reset -y    Same as 'reset' but skips the confirmation prompt (CI/test automation)."
  exit 0
fi

# --- Function to check PostgreSQL readiness ---
wait_for_postgres() {
  local max_attempts=10
  local attempt=1
  echo "⏳ Checking if PostgreSQL is ready..."
  until docker exec -u postgres $CONTAINER_NAME psql -d $DEFAULT_DB -c "SELECT 1;" >/dev/null 2>&1; do
    if [ $attempt -ge $max_attempts ]; then
      echo "❌ Error: PostgreSQL is not ready after $max_attempts attempts."
      exit 1
    fi
    echo "⏳ Attempt $attempt/$max_attempts: Waiting for PostgreSQL to be ready..."
    sleep 2
    ((attempt++))
  done
  echo "🟢 PostgreSQL is ready."
}

# --- Check if container exists ---
if docker ps -a --format '{{.Names}}' | grep -Eq "^${CONTAINER_NAME}$"; then
  if ! docker ps --format '{{.Names}}' | grep -Eq "^${CONTAINER_NAME}$"; then
    echo "🔄 Starting existing container '${CONTAINER_NAME}'..."
    docker start "$CONTAINER_NAME"
    wait_for_postgres
  else
    echo "🟢 PostgreSQL container '${CONTAINER_NAME}' is already running."
    wait_for_postgres
  fi
else
  if ! docker volume ls | grep -q "$DATA_VOLUME"; then
    docker volume create "$DATA_VOLUME"
  fi

  echo "🚀 Launching new PostgreSQL container '${CONTAINER_NAME}'..."
  docker run -d \
    --name $CONTAINER_NAME \
    -e POSTGRES_USER=$POSTGRES_USER \
    -e POSTGRES_PASSWORD=$POSTGRES_PASSWORD \
    -e POSTGRES_DB=$DEFAULT_DB \
    -p $HOST_PORT:5432 \
    -v $DATA_VOLUME:/var/lib/postgresql/data \
    postgis/postgis:16-3.5

  wait_for_postgres

  echo "📦 Installing pgvector extension..."
  docker exec -u root $CONTAINER_NAME bash -c "apt-get update && apt-get install -y postgresql-16-pgvector"
fi

# --- Fix collation ---
echo "🔧 Refreshing collation versions..."
docker exec -u postgres $CONTAINER_NAME psql -d $DEFAULT_DB -c "ALTER DATABASE postgres REFRESH COLLATION VERSION;" || true
docker exec -u postgres $CONTAINER_NAME psql -d template1 -c "ALTER DATABASE template1 REFRESH COLLATION VERSION;" || true

# --- Reset action: DESTRUCTIVE, drops and recreates the target database, then
# fully re-applies database-schema.sql. For local dev / unit testing only, so
# schema changes never rely on the fragile SCHEMA_VERSION/ALTER TABLE upgrade
# path against a stale already-provisioned database.
if [[ "$ACTION" == "reset" ]]; then
  if [[ "$SKIP_CONFIRM" != "true" ]]; then
    echo "⚠️  This will PERMANENTLY DROP database '${TARGET_DB}' in container '${CONTAINER_NAME}' and all its data."
    read -r -p "Type 'yes' to confirm: " CONFIRM_ANSWER
    if [[ "$CONFIRM_ANSWER" != "yes" ]]; then
      echo "❌ Aborted. No changes made."
      exit 1
    fi
  fi

  echo "🔌 Terminating existing connections to '${TARGET_DB}'..."
  docker exec -u postgres $CONTAINER_NAME psql -d $DEFAULT_DB -c "
    SELECT pg_terminate_backend(pid) FROM pg_stat_activity
    WHERE datname = '${TARGET_DB}' AND pid <> pg_backend_pid();
  " >/dev/null

  echo "🗑️  Dropping database '${TARGET_DB}'..."
  docker exec -u postgres $CONTAINER_NAME psql -d $DEFAULT_DB -c "DROP DATABASE IF EXISTS ${TARGET_DB};"

  echo "🚀 Creating fresh database '${TARGET_DB}'..."
  docker exec -u postgres $CONTAINER_NAME psql -d $DEFAULT_DB -c "CREATE DATABASE ${TARGET_DB};"

  wait_for_postgres_target() {
    local max_attempts=5
    local attempt=1
    echo "⏳ Checking if database '${TARGET_DB}' is accessible..."
    until docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "SELECT 1;" >/dev/null 2>&1; do
      if [ $attempt -ge $max_attempts ]; then
        echo "❌ Error: Database '${TARGET_DB}' is not accessible."
        exit 1
      fi
      echo "⏳ Attempt $attempt/$max_attempts: Waiting..."
      sleep 2
      ((attempt++))
    done
    echo "🟢 Database '${TARGET_DB}' is accessible."
  }
  wait_for_postgres_target

  echo "🔧 Enabling extensions in '${TARGET_DB}'..."
  docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "CREATE EXTENSION IF NOT EXISTS vector;"
  docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "CREATE EXTENSION IF NOT EXISTS postgis;"

  if [ ! -f "$SCHEMA_FILE" ]; then
    echo "❌ Error: Schema file '$SCHEMA_FILE' not found."
    exit 1
  fi

  echo "📜 Recreating schema_migrations tracking table..."
  docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "
  CREATE TABLE IF NOT EXISTS schema_migrations (
      version INT PRIMARY KEY,
      applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
  );
  "

  echo "⚡ Applying '$SCHEMA_FILE' fresh (full reset)..."
  docker cp "$SCHEMA_FILE" $CONTAINER_NAME:/tmp/database-schema.sql
  docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -v ON_ERROR_STOP=1 -f /tmp/database-schema.sql || {
    echo "❌ Error: Failed to apply schema during reset."
    exit 1
  }
  docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "INSERT INTO schema_migrations(version) VALUES ($SCHEMA_VERSION) ON CONFLICT (version) DO NOTHING;"

  echo "✅ Database '${TARGET_DB}' reset complete and schema v$SCHEMA_VERSION applied fresh."
  exit 0
fi

# --- Create DB if missing ---
echo "🔄 Checking if database '${TARGET_DB}' exists..."
docker exec -u postgres $CONTAINER_NAME psql -d $DEFAULT_DB -tc "SELECT 1 FROM pg_database WHERE datname = '${TARGET_DB}'" | grep -q 1 || {
  echo "🚀 Creating database '${TARGET_DB}'..."
  docker exec -u postgres $CONTAINER_NAME psql -d $DEFAULT_DB -c "CREATE DATABASE ${TARGET_DB};"
}

# --- Ensure connection ---
wait_for_postgres_target() {
  local max_attempts=5
  local attempt=1
  echo "⏳ Checking if database '${TARGET_DB}' is accessible..."
  until docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "SELECT 1;" >/dev/null 2>&1; do
    if [ $attempt -ge $max_attempts ]; then
      echo "❌ Error: Database '${TARGET_DB}' is not accessible."
      exit 1
    fi
    echo "⏳ Attempt $attempt/$max_attempts: Waiting..."
    sleep 2
    ((attempt++))
  done
  echo "🟢 Database '${TARGET_DB}' is accessible."
}
wait_for_postgres_target

# --- Enable extensions ---
echo "🔧 Enabling extensions in '${TARGET_DB}'..."
docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "CREATE EXTENSION IF NOT EXISTS vector;"
docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "CREATE EXTENSION IF NOT EXISTS postgis;"

# --- Schema migrations tracking ---
echo "📜 Ensuring schema_migrations table exists..."
docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "
CREATE TABLE IF NOT EXISTS schema_migrations (
    version INT PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
"

# --- Run schema migration only if needed ---
if [ -f "$SCHEMA_FILE" ]; then
  applied=$(docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -tAc "SELECT 1 FROM schema_migrations WHERE version = $SCHEMA_VERSION;")

  if [ "$applied" != "1" ]; then
    echo "⚡ Running schema migration v$SCHEMA_VERSION..."
    docker cp "$SCHEMA_FILE" $CONTAINER_NAME:/tmp/database-schema.sql
    docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -v ON_ERROR_STOP=1 -f /tmp/database-schema.sql || {
      echo "❌ Error: Failed to run schema migration."
      exit 1
    }
    docker exec -u postgres $CONTAINER_NAME psql -d $TARGET_DB -c "INSERT INTO schema_migrations(version) VALUES ($SCHEMA_VERSION);"
    echo "✅ Migration v$SCHEMA_VERSION applied."
  else
    echo "🟢 Schema migration v$SCHEMA_VERSION already applied. Skipping."
  fi
else
  echo "⚠️ Warning: Schema file '$SCHEMA_FILE' not found."
fi
