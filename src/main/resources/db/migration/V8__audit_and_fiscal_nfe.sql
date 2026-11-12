-- ==============================================================================
-- V8__audit_and_fiscal_nfe.sql
-- Sprint 6: Observabilidade, Auditoria Imutável (LGPD/PCI-DSS) e Homologação Fiscal SEFAZ (NF-e 55 e NFC-e 65)
-- Plataforma de E-commerce, PDV e Gestão Multi-Tenant
-- ==============================================================================

SET LOCAL app.bypass_rls = 'true';

-- 1. Tabela de Trilha de Auditoria Imutável (Audit Logs) com RLS e Compliance LGPD/PCI-DSS
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    user_email VARCHAR(255),
    user_role VARCHAR(50),
    client_ip VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100),
    details_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_created ON audit_logs(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_action ON audit_logs(tenant_id, action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_resource ON audit_logs(tenant_id, resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_user ON audit_logs(tenant_id, user_id);

ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs FORCE ROW LEVEL SECURITY;

CREATE POLICY audit_logs_tenant_isolation_policy ON audit_logs
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- Imutabilidade do log de auditoria: proibir atualizações e remoções
CREATE OR REPLACE RULE no_update_audit_logs AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE OR REPLACE RULE no_delete_audit_logs AS ON DELETE TO audit_logs DO INSTEAD NOTHING;

-- 2. Tabela de Controle Sequencial de Numeração Fiscal (Gap-Free Serial Numbering)
CREATE TABLE IF NOT EXISTS fiscal_numbering_sequences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    model VARCHAR(10) NOT NULL,
    series INT NOT NULL DEFAULT 1,
    current_number BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_fiscal_numbering_seq UNIQUE (tenant_id, company_id, model, series)
);

CREATE INDEX IF NOT EXISTS idx_fiscal_seq_tenant_comp ON fiscal_numbering_sequences(tenant_id, company_id);

ALTER TABLE fiscal_numbering_sequences ENABLE ROW LEVEL SECURITY;
ALTER TABLE fiscal_numbering_sequences FORCE ROW LEVEL SECURITY;

CREATE POLICY fiscal_numbering_tenant_isolation_policy ON fiscal_numbering_sequences
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 3. Tabela Central de Documentos Fiscais Eletrônicos (NF-e Modelo 55 e NFC-e Modelo 65)
CREATE TABLE IF NOT EXISTS fiscal_invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    sale_id UUID REFERENCES pos_sales(id) ON DELETE SET NULL,
    model VARCHAR(10) NOT NULL, -- '55' (NF-e) ou '65' (NFC-e)
    series INT NOT NULL DEFAULT 1,
    invoice_number BIGINT NOT NULL,
    access_key VARCHAR(44) NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'EMITIDA', -- EMITIDA, AUTORIZADA, CANCELADA, CONTINGENCIA, REJEITADA
    nature_of_operation VARCHAR(100) NOT NULL DEFAULT 'VENDA DE MERCADORIAS',
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_icms NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_pis NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_tax_cofins NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    protocol_number VARCHAR(100),
    xml_payload TEXT,
    danfe_url VARCHAR(500),
    rejection_reason TEXT,
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    authorized_at TIMESTAMP WITH TIME ZONE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_fiscal_invoices_number UNIQUE (tenant_id, company_id, model, series, invoice_number)
);

CREATE INDEX IF NOT EXISTS idx_fiscal_invoices_tenant_id ON fiscal_invoices(tenant_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_invoices_company_id ON fiscal_invoices(company_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_invoices_order_id ON fiscal_invoices(order_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_invoices_sale_id ON fiscal_invoices(sale_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_invoices_access_key ON fiscal_invoices(access_key);
CREATE INDEX IF NOT EXISTS idx_fiscal_invoices_status ON fiscal_invoices(status);
CREATE INDEX IF NOT EXISTS idx_fiscal_invoices_created_at ON fiscal_invoices(created_at DESC);

ALTER TABLE fiscal_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE fiscal_invoices FORCE ROW LEVEL SECURITY;

CREATE POLICY fiscal_invoices_tenant_isolation_policy ON fiscal_invoices
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 4. Novas Permissões de Sistema para Fiscal e Auditoria
INSERT INTO permissions (id, description, category) VALUES
    ('FISCAL_CANCEL', 'Cancelar NF-e e NFC-e perante a SEFAZ', 'FISCAL'),
    ('AUDIT_VIEW', 'Visualizar trilha de auditoria e conformidade LGPD', 'SECURITY')
ON CONFLICT (id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('STORE_MANAGER', 'FISCAL_CANCEL'),
    ('STORE_MANAGER', 'AUDIT_VIEW'),
    ('TENANT_ADMIN', 'FISCAL_CANCEL'),
    ('TENANT_ADMIN', 'AUDIT_VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;
