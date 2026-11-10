-- ==============================================================================
-- V2__seed_initial_tenant.sql
-- Inserção de Tenant e Usuário Administrador de Desenvolvimento/Demonstração
-- ==============================================================================

SET LOCAL app.bypass_rls = 'true';

-- 1. Tenant Inicial (Livraria Matriz)
INSERT INTO tenants (
    id,
    slug,
    name,
    trade_name,
    cnpj,
    email,
    phone,
    status,
    settings_json
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'matriz',
    'Livraria Matriz E-commerce e PDV',
    'Livraria Matriz',
    '12345678000190',
    'contato@livrariamatriz.com.br',
    '11999998888',
    'ACTIVE',
    '{"primary_color": "#1e3a8a", "secondary_color": "#3b82f6", "allow_installments": true, "max_installments": 12}'::jsonb
) ON CONFLICT (slug) DO NOTHING;

-- 2. Domínio Inicial para testes locais
INSERT INTO tenant_domains (
    id,
    tenant_id,
    domain,
    is_primary
) VALUES (
    '00000000-0000-0000-0000-000000000010',
    '00000000-0000-0000-0000-000000000001',
    'localhost',
    true
) ON CONFLICT (domain) DO NOTHING;

-- 3. Usuário Administrador Inicial do Tenant
-- Senha padrão para testes de desenvolvimento: Admin@123456
-- Hash BCrypt gerado com custo 10
INSERT INTO users (
    id,
    tenant_id,
    name,
    email,
    password_hash,
    role,
    active
) VALUES (
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000001',
    'Administrador Matriz',
    'admin@livrariamatriz.com.br',
    '$2a$10$7ZkE0B73h3u8d31a5K11bO8vJz67V8T6lGf0uO7yZz2A9D1C3e4Gy',
    'TENANT_ADMIN',
    true
) ON CONFLICT (tenant_id, email) DO NOTHING;

