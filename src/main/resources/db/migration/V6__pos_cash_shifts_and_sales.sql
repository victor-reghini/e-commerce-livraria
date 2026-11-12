-- ==============================================================================
-- V6__pos_cash_shifts_and_sales.sql
-- PDV Mobile e Venda Balcão / Feira:
-- 1. Controle de Caixa (Turno, Abertura, Suprimento, Sangria e Fechamento Cego)
-- 2. Movimentações Financeiras de Caixa (Auditoria de Caixa)
-- 3. Vendas de Balcão e Feira (POS Sales, Itens, Múltiplos Pagamentos)
-- 4. Suporte a Vendas Offline (Chave de Idempotência client_uuid)
-- 5. Estrutura de Cupom e NFC-e Modelo 65
-- ==============================================================================

SET LOCAL app.bypass_rls = 'true';

-- 1. Tabela de Turnos de Caixa (Cash Shifts)
CREATE TABLE IF NOT EXISTS cash_shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    cashier_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    shift_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ABERTO',
    opening_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    expected_balance NUMERIC(15, 2),
    actual_balance NUMERIC(15, 2),
    divergence NUMERIC(15, 2),
    notes TEXT,
    opened_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cash_shift_number UNIQUE (tenant_id, shift_number)
);

CREATE INDEX IF NOT EXISTS idx_cash_shifts_tenant_id ON cash_shifts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cash_shifts_company_id ON cash_shifts(company_id);
CREATE INDEX IF NOT EXISTS idx_cash_shifts_cashier_id ON cash_shifts(cashier_id);
CREATE INDEX IF NOT EXISTS idx_cash_shifts_status ON cash_shifts(status);

ALTER TABLE cash_shifts ENABLE ROW LEVEL SECURITY;
ALTER TABLE cash_shifts FORCE ROW LEVEL SECURITY;

CREATE POLICY cash_shifts_tenant_isolation_policy ON cash_shifts
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 2. Movimentações Financeiras do Turno de Caixa (Suprimento, Sangria, Venda, Estorno)
CREATE TABLE IF NOT EXISTS cash_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    shift_id UUID NOT NULL REFERENCES cash_shifts(id) ON DELETE CASCADE,
    movement_type VARCHAR(32) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    payment_method VARCHAR(32) NOT NULL DEFAULT 'DINHEIRO',
    reason VARCHAR(255),
    operator_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cash_movements_tenant_id ON cash_movements(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_shift_id ON cash_movements(shift_id);
CREATE INDEX IF NOT EXISTS idx_cash_movements_type ON cash_movements(movement_type);

ALTER TABLE cash_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE cash_movements FORCE ROW LEVEL SECURITY;

CREATE POLICY cash_movements_tenant_isolation_policy ON cash_movements
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 3. Vendas Realizadas no PDV (Balcão / Stand de Feira)
CREATE TABLE IF NOT EXISTS pos_sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    shift_id UUID NOT NULL REFERENCES cash_shifts(id) ON DELETE RESTRICT,
    sale_number VARCHAR(64) NOT NULL,
    client_uuid UUID,
    customer_name VARCHAR(255),
    customer_cpf_cnpj VARCHAR(20),
    subtotal NUMERIC(15, 2) NOT NULL,
    discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(15, 2) NOT NULL,
    total_paid NUMERIC(15, 2) NOT NULL,
    change_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'CONCLUIDA',
    nfce_key VARCHAR(64),
    nfce_xml TEXT,
    nfce_status VARCHAR(32) NOT NULL DEFAULT 'EMITIDA',
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pos_sales_tenant_number UNIQUE (tenant_id, sale_number),
    CONSTRAINT uk_pos_sales_client_uuid UNIQUE (tenant_id, client_uuid)
);

CREATE INDEX IF NOT EXISTS idx_pos_sales_tenant_id ON pos_sales(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pos_sales_company_id ON pos_sales(company_id);
CREATE INDEX IF NOT EXISTS idx_pos_sales_shift_id ON pos_sales(shift_id);
CREATE INDEX IF NOT EXISTS idx_pos_sales_client_uuid ON pos_sales(client_uuid);
CREATE INDEX IF NOT EXISTS idx_pos_sales_created_at ON pos_sales(created_at);

ALTER TABLE pos_sales ENABLE ROW LEVEL SECURITY;
ALTER TABLE pos_sales FORCE ROW LEVEL SECURITY;

CREATE POLICY pos_sales_tenant_isolation_policy ON pos_sales
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 4. Itens da Venda PDV
CREATE TABLE IF NOT EXISTS pos_sale_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    sale_id UUID NOT NULL REFERENCES pos_sales(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES skus(id) ON DELETE RESTRICT,
    quantity INT NOT NULL,
    unit_price NUMERIC(15, 2) NOT NULL,
    discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    subtotal NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pos_sale_items_tenant_id ON pos_sale_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pos_sale_items_sale_id ON pos_sale_items(sale_id);
CREATE INDEX IF NOT EXISTS idx_pos_sale_items_sku_id ON pos_sale_items(sku_id);

ALTER TABLE pos_sale_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE pos_sale_items FORCE ROW LEVEL SECURITY;

CREATE POLICY pos_sale_items_tenant_isolation_policy ON pos_sale_items
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 5. Pagamentos da Venda PDV (Múltiplas Formas de Pagamento em uma mesma venda)
CREATE TABLE IF NOT EXISTS pos_sale_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    sale_id UUID NOT NULL REFERENCES pos_sales(id) ON DELETE CASCADE,
    payment_method VARCHAR(32) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pos_sale_payments_tenant_id ON pos_sale_payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pos_sale_payments_sale_id ON pos_sale_payments(sale_id);

ALTER TABLE pos_sale_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE pos_sale_payments FORCE ROW LEVEL SECURITY;

CREATE POLICY pos_sale_payments_tenant_isolation_policy ON pos_sale_payments
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());
