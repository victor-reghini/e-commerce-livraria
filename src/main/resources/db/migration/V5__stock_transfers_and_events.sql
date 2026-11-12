-- ==============================================================================
-- V5__stock_transfers_and_events.sql
-- Múltiplos Estoques, Transferências entre Filiais/Feiras, Romaneio de Remessa,
-- Histórico de Movimentações (Ledger) e Conferência Cega (Bipagem de Retorno)
-- ==============================================================================

SET LOCAL app.bypass_rls = 'true';

-- 1. Tabela de Transferências de Estoque (Remessas para Feiras, Retornos e Transferências entre Filiais)
CREATE TABLE IF NOT EXISTS stock_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    transfer_number VARCHAR(64) NOT NULL,
    origin_company_id UUID NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    destination_company_id UUID NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'CRIADO',
    transfer_type VARCHAR(32) NOT NULL DEFAULT 'REMESSA_EVENTO',
    notes TEXT,
    dispatched_at TIMESTAMP WITH TIME ZONE,
    received_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    received_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_transfer_tenant_number UNIQUE (tenant_id, transfer_number)
);

CREATE INDEX IF NOT EXISTS idx_stock_transfers_tenant_id ON stock_transfers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_transfers_origin ON stock_transfers(origin_company_id);
CREATE INDEX IF NOT EXISTS idx_stock_transfers_destination ON stock_transfers(destination_company_id);
CREATE INDEX IF NOT EXISTS idx_stock_transfers_status ON stock_transfers(status);

ALTER TABLE stock_transfers ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_transfers FORCE ROW LEVEL SECURITY;

CREATE POLICY stock_transfers_tenant_isolation_policy ON stock_transfers
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 2. Itens da Transferência de Estoque
CREATE TABLE IF NOT EXISTS stock_transfer_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    transfer_id UUID NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES skus(id) ON DELETE RESTRICT,
    quantity_requested INT NOT NULL,
    quantity_dispatched INT NOT NULL DEFAULT 0,
    quantity_received INT NOT NULL DEFAULT 0,
    unit_cost NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    unit_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_transfer_item_sku UNIQUE (transfer_id, sku_id)
);

CREATE INDEX IF NOT EXISTS idx_transfer_items_tenant_id ON stock_transfer_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_transfer_items_transfer_id ON stock_transfer_items(transfer_id);
CREATE INDEX IF NOT EXISTS idx_transfer_items_sku_id ON stock_transfer_items(sku_id);

ALTER TABLE stock_transfer_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_transfer_items FORCE ROW LEVEL SECURITY;

CREATE POLICY stock_transfer_items_tenant_isolation_policy ON stock_transfer_items
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 3. Histórico de Movimentações de Estoque (Audit Trail / Ledger Imutável)
CREATE TABLE IF NOT EXISTS stock_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES skus(id) ON DELETE RESTRICT,
    movement_type VARCHAR(32) NOT NULL,
    quantity_change INT NOT NULL,
    balance_after INT NOT NULL,
    reference_id UUID,
    reason VARCHAR(255),
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_tenant_id ON stock_movements(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_company_sku ON stock_movements(company_id, sku_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON stock_movements(created_at);

ALTER TABLE stock_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_movements FORCE ROW LEVEL SECURITY;

CREATE POLICY stock_movements_tenant_isolation_policy ON stock_movements
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 4. Tabela de Conferência Cega de Retorno (Bipagem / Blind Audit)
CREATE TABLE IF NOT EXISTS blind_audits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    transfer_id UUID REFERENCES stock_transfers(id) ON DELETE SET NULL,
    code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ABERTA',
    auditor_id UUID,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_blind_audit_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_blind_audits_tenant_id ON blind_audits(tenant_id);
CREATE INDEX IF NOT EXISTS idx_blind_audits_company_id ON blind_audits(company_id);
CREATE INDEX IF NOT EXISTS idx_blind_audits_transfer_id ON blind_audits(transfer_id);

ALTER TABLE blind_audits ENABLE ROW LEVEL SECURITY;
ALTER TABLE blind_audits FORCE ROW LEVEL SECURITY;

CREATE POLICY blind_audits_tenant_isolation_policy ON blind_audits
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 5. Itens da Conferência Cega (Contagem Bipada x Saldo Esperado)
CREATE TABLE IF NOT EXISTS blind_audit_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    audit_id UUID NOT NULL REFERENCES blind_audits(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES skus(id) ON DELETE RESTRICT,
    quantity_scanned INT NOT NULL DEFAULT 0,
    quantity_expected INT NOT NULL DEFAULT 0,
    divergence INT NOT NULL DEFAULT 0,
    last_scanned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_audit_item_sku UNIQUE (audit_id, sku_id)
);

CREATE INDEX IF NOT EXISTS idx_blind_audit_items_tenant_id ON blind_audit_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_blind_audit_items_audit_id ON blind_audit_items(audit_id);
CREATE INDEX IF NOT EXISTS idx_blind_audit_items_sku_id ON blind_audit_items(sku_id);

ALTER TABLE blind_audit_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE blind_audit_items FORCE ROW LEVEL SECURITY;

CREATE POLICY blind_audit_items_tenant_isolation_policy ON blind_audit_items
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());
