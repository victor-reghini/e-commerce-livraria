-- ==============================================================================
-- V3__companies_and_rbac.sql
-- Modelagem de Empresas / Filiais (Lojas, PDV, Feiras) e Permissões Granulares (RBAC)
-- ==============================================================================

SET LOCAL app.bypass_rls = 'true';

-- 1. Tabela de Empresas / Filiais / Unidades de Negócio do Tenant
CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    cnpj VARCHAR(18),
    state_registration VARCHAR(32),
    municipal_registration VARCHAR(32),
    tax_regime VARCHAR(32) NOT NULL DEFAULT 'SIMPLES_NACIONAL',
    is_headquarters BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    address_json JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_company_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_companies_tenant_id ON companies(tenant_id);
CREATE INDEX IF NOT EXISTS idx_companies_code ON companies(code);

-- Habilitação de RLS em companies
ALTER TABLE companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE companies FORCE ROW LEVEL SECURITY;

CREATE POLICY companies_tenant_isolation_policy ON companies
    AS PERMISSIVE
    FOR ALL
    USING (
        is_bypass_rls() OR tenant_id = current_app_tenant()
    )
    WITH CHECK (
        is_bypass_rls() OR tenant_id = current_app_tenant()
    );

-- 2. Tabela de Permissões do Sistema (RBAC)
CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(64) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL
);

-- 3. Tabela de Perfis / Roles
CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255)
);

-- 4. Associação Role x Permissões
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id VARCHAR(50) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id VARCHAR(64) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 5. Tabela de Vínculo de Usuários às Empresas (Lojas / Feiras permitidas)
CREATE TABLE IF NOT EXISTS user_companies (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, company_id)
);

CREATE INDEX IF NOT EXISTS idx_user_companies_user_id ON user_companies(user_id);
CREATE INDEX IF NOT EXISTS idx_user_companies_tenant_id ON user_companies(tenant_id);

-- Habilitação de RLS em user_companies
ALTER TABLE user_companies ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_companies FORCE ROW LEVEL SECURITY;

CREATE POLICY user_companies_tenant_isolation_policy ON user_companies
    AS PERMISSIVE
    FOR ALL
    USING (
        is_bypass_rls() OR tenant_id = current_app_tenant()
    )
    WITH CHECK (
        is_bypass_rls() OR tenant_id = current_app_tenant()
    );

-- ==============================================================================
-- Seeds de Permissões e Perfis Padrão
-- ==============================================================================

-- Inserção das Permissões do Sistema
INSERT INTO permissions (id, description, category) VALUES
    -- Catálogo
    ('CATALOG_READ', 'Visualizar produtos, categorias e kits', 'CATALOG'),
    ('CATALOG_WRITE', 'Cadastrar e editar produtos, variações e kits', 'CATALOG'),
    ('CATALOG_IMPORT', 'Importar planilhas em lote de catálogo', 'CATALOG'),
    -- Estoque
    ('STOCK_READ', 'Visualizar saldos de estoque por localidade', 'STOCK'),
    ('STOCK_WRITE', 'Ajustar saldos e movimentar estoque', 'STOCK'),
    ('STOCK_TRANSFER', 'Romaneio e transferência entre matriz, balcão e eventos', 'STOCK'),
    -- Pedidos & Vendas
    ('ORDER_READ', 'Visualizar pedidos e orçamentos', 'ORDER'),
    ('ORDER_WRITE', 'Criar e atualizar pedidos', 'ORDER'),
    ('ORDER_CANCEL', 'Cancelar pedidos e estornar transações', 'ORDER'),
    -- PDV Balcão & Eventos
    ('PDV_SELL', 'Realizar vendas no PDV móvel/balcão', 'PDV'),
    ('PDV_DISCOUNT', 'Aplicar descontos especiais no PDV', 'PDV'),
    ('PDV_CASH_CONTROL', 'Abertura, sangria e fechamento de caixa', 'PDV'),
    -- Fiscal
    ('FISCAL_EMIT_NFE', 'Emitir NF-e (modelo 55) para e-commerce e faturamento', 'FISCAL'),
    ('FISCAL_EMIT_NFCE', 'Emitir NFC-e (modelo 65) no PDV', 'FISCAL'),
    -- Gestão e Segurança
    ('TENANT_MANAGE', 'Configurar preferências visuais e dados do tenant', 'MANAGEMENT'),
    ('COMPANY_MANAGE', 'Cadastrar e gerenciar filiais, balcões e feiras', 'MANAGEMENT'),
    ('USER_MANAGE', 'Gerenciar usuários, papéis e permissões de acesso', 'SECURITY'),
    ('FINANCIAL_VIEW', 'Visualizar relatórios financeiros e conciliação', 'FINANCIAL')
ON CONFLICT (id) DO NOTHING;

-- Inserção dos Papéis
INSERT INTO roles (id, name, description) VALUES
    ('SUPER_ADMIN', 'Super Administrador da Plataforma', 'Acesso irrestrito a todos os tenants e configurações globais'),
    ('TENANT_ADMIN', 'Administrador da Livraria/Lojista', 'Controle total sobre o tenant, usuários, filiais e financeiro'),
    ('STORE_MANAGER', 'Gerente de Loja / Evento', 'Gestão de catálogo, estoque, pedidos e equipes da unidade'),
    ('ATTENDANT', 'Atendente / Operador de Caixa PDV', 'Operação de caixa, bipagem de produtos e vendas no balcão ou feira'),
    ('CUSTOMER', 'Cliente E-commerce', 'Acesso ao catálogo público, carrinho, checkout e meus pedidos')
ON CONFLICT (id) DO NOTHING;

-- Associação: TENANT_ADMIN possui todas as permissões
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'TENANT_ADMIN', id FROM permissions
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Associação: STORE_MANAGER
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('STORE_MANAGER', 'CATALOG_READ'),
    ('STORE_MANAGER', 'CATALOG_WRITE'),
    ('STORE_MANAGER', 'STOCK_READ'),
    ('STORE_MANAGER', 'STOCK_WRITE'),
    ('STORE_MANAGER', 'STOCK_TRANSFER'),
    ('STORE_MANAGER', 'ORDER_READ'),
    ('STORE_MANAGER', 'ORDER_WRITE'),
    ('STORE_MANAGER', 'PDV_SELL'),
    ('STORE_MANAGER', 'PDV_DISCOUNT'),
    ('STORE_MANAGER', 'PDV_CASH_CONTROL'),
    ('STORE_MANAGER', 'FISCAL_EMIT_NFE'),
    ('STORE_MANAGER', 'FISCAL_EMIT_NFCE'),
    ('STORE_MANAGER', 'FINANCIAL_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Associação: ATTENDANT (Operador de PDV)
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('ATTENDANT', 'CATALOG_READ'),
    ('ATTENDANT', 'STOCK_READ'),
    ('ATTENDANT', 'ORDER_READ'),
    ('ATTENDANT', 'ORDER_WRITE'),
    ('ATTENDANT', 'PDV_SELL'),
    ('ATTENDANT', 'PDV_CASH_CONTROL'),
    ('ATTENDANT', 'FISCAL_EMIT_NFCE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Associação: CUSTOMER (Cliente final da loja virtual)
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('CUSTOMER', 'CATALOG_READ'),
    ('CUSTOMER', 'ORDER_READ'),
    ('CUSTOMER', 'ORDER_WRITE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 6. Inserção de Empresa Matriz Inicial para o Tenant Default
INSERT INTO companies (
    id,
    tenant_id,
    code,
    name,
    trade_name,
    cnpj,
    state_registration,
    tax_regime,
    is_headquarters,
    active,
    address_json
) VALUES (
    '00000000-0000-0000-0000-000000000500',
    '00000000-0000-0000-0000-000000000001',
    'MATRIZ',
    'Livraria Matriz Ltda',
    'Livraria Matriz - Loja Principal & E-commerce',
    '12345678000190',
    '123456789012',
    'SIMPLES_NACIONAL',
    true,
    true,
    '{"street": "Av. Paulista", "number": "1000", "complement": "Loja 5", "neighborhood": "Bela Vista", "city": "São Paulo", "state": "SP", "zip_code": "01310-100"}'::jsonb
) ON CONFLICT (tenant_id, code) DO NOTHING;

-- 7. Vínculo do usuário admin default à empresa matriz
INSERT INTO user_companies (
    user_id,
    company_id,
    tenant_id
) VALUES (
    '00000000-0000-0000-0000-000000000100',
    '00000000-0000-0000-0000-000000000500',
    '00000000-0000-0000-0000-000000000001'
) ON CONFLICT (user_id, company_id) DO NOTHING;

