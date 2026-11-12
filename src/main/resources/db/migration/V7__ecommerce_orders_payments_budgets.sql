-- ==============================================================================
-- V7: E-Commerce, Carrinho, Pedidos, Pagar.me V5 e Vendas a Prazo (Orçamentos)
-- Plataforma de E-commerce, PDV e Gestão Multi-Tenant
-- ==============================================================================

-- 1. Carrinhos de Compras (Session-based e Customer-based)
CREATE TABLE carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    session_id VARCHAR(100),
    customer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

ALTER TABLE carts ENABLE ROW LEVEL SECURITY;
ALTER TABLE carts FORCE ROW LEVEL SECURITY;

CREATE POLICY carts_tenant_isolation_policy ON carts
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

CREATE INDEX idx_carts_tenant_session ON carts(tenant_id, session_id);
CREATE INDEX idx_carts_tenant_customer ON carts(tenant_id, customer_id);

-- 2. Itens do Carrinho
CREATE TABLE cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_cart_items_cart_sku UNIQUE (cart_id, sku_id)
);

ALTER TABLE cart_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE cart_items FORCE ROW LEVEL SECURITY;

CREATE POLICY cart_items_tenant_isolation_policy ON cart_items
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

CREATE INDEX idx_cart_items_tenant_cart ON cart_items(tenant_id, cart_id);

-- 3. Pedidos de Venda E-Commerce & Venda a Prazo
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_number VARCHAR(50) NOT NULL,
    customer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_document VARCHAR(50) NOT NULL,
    customer_phone VARCHAR(50),
    origin_company_id UUID REFERENCES companies(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AGUARDANDO_PAGAMENTO',
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    shipping_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    shipping_carrier VARCHAR(100),
    shipping_service VARCHAR(100),
    tracking_code VARCHAR(100),
    tracking_url VARCHAR(500),
    shipping_address_street VARCHAR(255),
    shipping_address_number VARCHAR(50),
    shipping_address_complement VARCHAR(255),
    shipping_address_neighborhood VARCHAR(100),
    shipping_address_city VARCHAR(100),
    shipping_address_state VARCHAR(2),
    shipping_address_zip_code VARCHAR(20),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP WITH TIME ZONE,
    shipped_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_orders_tenant_order_number UNIQUE (tenant_id, order_number)
);

ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders FORCE ROW LEVEL SECURITY;

CREATE POLICY orders_tenant_isolation_policy ON orders
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

CREATE INDEX idx_orders_tenant_status ON orders(tenant_id, status);
CREATE INDEX idx_orders_tenant_customer ON orders(tenant_id, customer_id);
CREATE INDEX idx_orders_tenant_document ON orders(tenant_id, customer_document);

-- 4. Itens do Pedido
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES skus(id),
    sku_code VARCHAR(100) NOT NULL,
    product_title VARCHAR(255) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL,
    discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(15, 2) NOT NULL,
    ncm VARCHAR(20),
    cfop VARCHAR(10)
);

ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items FORCE ROW LEVEL SECURITY;

CREATE POLICY order_items_tenant_isolation_policy ON order_items
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

CREATE INDEX idx_order_items_tenant_order ON order_items(tenant_id, order_id);

-- 5. Transações de Pagamento do Pedido (Pagar.me V5 & Faturamento)
CREATE TABLE order_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_method VARCHAR(50) NOT NULL,
    gateway VARCHAR(50) NOT NULL DEFAULT 'PAGARME',
    gateway_transaction_id VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDENTE',
    amount NUMERIC(15, 2) NOT NULL,
    installments INT NOT NULL DEFAULT 1,
    pix_qr_code TEXT,
    pix_copia_cola TEXT,
    boleto_barcode VARCHAR(100),
    boleto_url VARCHAR(500),
    boleto_due_date DATE,
    card_brand VARCHAR(50),
    card_last_four VARCHAR(4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    paid_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE order_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_payments FORCE ROW LEVEL SECURITY;

CREATE POLICY order_payments_tenant_isolation_policy ON order_payments
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

CREATE INDEX idx_order_payments_tenant_order ON order_payments(tenant_id, order_id);
CREATE INDEX idx_order_payments_gateway_tx ON order_payments(tenant_id, gateway_transaction_id);

-- 6. Orçamentos Corporativos / Faturamento Institucional
CREATE TABLE corporate_budgets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    budget_number VARCHAR(50) NOT NULL,
    customer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    corporate_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    cnpj VARCHAR(20) NOT NULL,
    state_registration VARCHAR(50),
    contact_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'RASCUNHO',
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    shipping_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    payment_terms_days INT NOT NULL DEFAULT 30,
    due_date DATE,
    notes TEXT,
    rejection_reason TEXT,
    invoiced_order_id UUID REFERENCES orders(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_budgets_tenant_number UNIQUE (tenant_id, budget_number)
);

ALTER TABLE corporate_budgets ENABLE ROW LEVEL SECURITY;
ALTER TABLE corporate_budgets FORCE ROW LEVEL SECURITY;

CREATE POLICY corporate_budgets_tenant_isolation_policy ON corporate_budgets
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

CREATE INDEX idx_budgets_tenant_cnpj ON corporate_budgets(tenant_id, cnpj);
CREATE INDEX idx_budgets_tenant_status ON corporate_budgets(tenant_id, status);

-- 7. Itens do Orçamento Corporativo
CREATE TABLE corporate_budget_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    budget_id UUID NOT NULL REFERENCES corporate_budgets(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES skus(id),
    sku_code VARCHAR(100) NOT NULL,
    product_title VARCHAR(255) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(15, 2) NOT NULL,
    discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total NUMERIC(15, 2) NOT NULL
);

ALTER TABLE corporate_budget_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE corporate_budget_items FORCE ROW LEVEL SECURITY;

CREATE POLICY corporate_budget_items_tenant_isolation_policy ON corporate_budget_items
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

CREATE INDEX idx_budget_items_tenant_budget ON corporate_budget_items(tenant_id, budget_id);

-- 8. Tabela de Webhooks Processados (Idempotência Estrita de Gateways)
CREATE TABLE processed_webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    gateway VARCHAR(50) NOT NULL DEFAULT 'PAGARME',
    event_id VARCHAR(150) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PROCESSED',
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_processed_webhooks_gateway_event UNIQUE (tenant_id, gateway, event_id)
);

ALTER TABLE processed_webhooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE processed_webhooks FORCE ROW LEVEL SECURITY;

CREATE POLICY processed_webhooks_tenant_isolation_policy ON processed_webhooks
    FOR ALL
    USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);

CREATE INDEX idx_webhooks_tenant_event ON processed_webhooks(tenant_id, gateway, event_id);
