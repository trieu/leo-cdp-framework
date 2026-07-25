SELECT 'CREATE DATABASE db_keycloak' 
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'db_keycloak')\gexec
