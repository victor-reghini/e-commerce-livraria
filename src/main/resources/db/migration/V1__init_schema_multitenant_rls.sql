-- ==============================================================================
-- V1__init_schema_multitenant_rls.sql
-- Setup de Extensões, Funções de RLS e Tabelas Fundamentais de Multi-Tenancy
-- ==============================================================================

-- 1. Extensões essenciais
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Funções auxiliares para Row-Level Security (RLS)
-- Retorna o UUID do tenant definido na sessão atual da conexão via SET LOCAL app.current_tenant = 'uuid'
CREATE OR REPLACE FUNCTION current_app_tenant() RETURNS uuid AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_tenant', true), '')::uuid;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END;
$$ LANGUAGE plpgsql STABLE;

-- Permite bypass de RLS para tarefas administrativas/migrações via SET LOCAL app.bypass_rls = 'true'
CREATE OR REPLACE FUNCTION is_bypass_rls() RETURNS boolean AS $$
BEGIN
    RETURN COALESCE(current_setting('app.bypass_rls', true), 'false') = 'true';
END;
$$ LANGUAGE plpgsql STABLE;

-- 3. Tabela de Tenants (Lojistas / Empresas do ecossistema)
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    cnpj VARCHAR(18) UNIQUE,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    settings_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);

-- 4. Tabela de Domínios vinculados aos Tenants (para resolução automática de subdomínios e domínios próprios)
CREATE TABLE IF NOT EXISTS tenant_domains (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    domain VARCHAR(255) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenant_domains_domain ON tenant_domains(domain);
CREATE INDEX IF NOT EXISTS idx_tenant_domains_tenant_id ON tenant_domains(tenant_id);

-- 5. Tabela de Usuários com Isolamento Estrito de RLS
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 6. Habilitação de Row-Level Security (RLS) nas tabelas isoladas por Tenant
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

-- 7. Criação da Política de RLS para Usuários
CREATE POLICY users_tenant_isolation_policy ON users
    AS PERMISSIVE
    FOR ALL
    USING (
        is_bypass_rls() OR tenant_id = current_app_tenant()
    )
    WITH CHECK (
        is_bypass_rls() OR tenant_id = current_app_tenant()
    );

