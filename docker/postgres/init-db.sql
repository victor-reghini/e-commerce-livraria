-- ==============================================================================
-- docker/postgres/init-db.sql
-- Script de inicialização automática executado pelo container PostgreSQL
-- ==============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
BEGIN
    RAISE NOTICE 'Extensões uuid-ossp e pgcrypto inicializadas com sucesso.';
END
$$;
