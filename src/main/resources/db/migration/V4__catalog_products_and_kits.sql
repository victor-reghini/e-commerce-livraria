-- ==============================================================================
-- V4__catalog_products_and_kits.sql
-- Catálogo de Produtos: Categorias, Autores, Editoras, Produtos Pai, SKUs/Variações,
-- Kits/Combos (Virtuais e Físicos), Metadados de E-book, Imagens e Estoque Base
-- ==============================================================================

SET LOCAL app.bypass_rls = 'true';

-- 1. Categorias de Produtos (com suporte a hierarquia)
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_category_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_categories_tenant_id ON categories(tenant_id);
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories FORCE ROW LEVEL SECURITY;

CREATE POLICY categories_tenant_isolation_policy ON categories
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 2. Autores / Criadores
CREATE TABLE IF NOT EXISTS authors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    biography TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_authors_tenant_id ON authors(tenant_id);
CREATE INDEX IF NOT EXISTS idx_authors_name ON authors(name);

ALTER TABLE authors ENABLE ROW LEVEL SECURITY;
ALTER TABLE authors FORCE ROW LEVEL SECURITY;

CREATE POLICY authors_tenant_isolation_policy ON authors
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 3. Editoras
CREATE TABLE IF NOT EXISTS publishers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_publishers_tenant_id ON publishers(tenant_id);
CREATE INDEX IF NOT EXISTS idx_publishers_name ON publishers(name);

ALTER TABLE publishers ENABLE ROW LEVEL SECURITY;
ALTER TABLE publishers FORCE ROW LEVEL SECURITY;

CREATE POLICY publishers_tenant_isolation_policy ON publishers
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 4. Produtos Pai (Metadados Globais: Título, Sinopse, Categorias, Editora)
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    publisher_id UUID REFERENCES publishers(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    subtitle VARCHAR(255),
    slug VARCHAR(255) NOT NULL,
    synopsis TEXT,
    product_type VARCHAR(50) NOT NULL DEFAULT 'BOOK',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_tenant_slug UNIQUE (tenant_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_products_tenant_id ON products(tenant_id);
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_publisher_id ON products(publisher_id);
CREATE INDEX IF NOT EXISTS idx_products_slug ON products(slug);
CREATE INDEX IF NOT EXISTS idx_products_product_type ON products(product_type);

ALTER TABLE products ENABLE ROW LEVEL SECURITY;
ALTER TABLE products FORCE ROW LEVEL SECURITY;

CREATE POLICY products_tenant_isolation_policy ON products
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 5. Relação Produto x Autores (com papel: AUTOR, TRADUTOR, ILUSTRADOR, ORGANIZADOR)
CREATE TABLE IF NOT EXISTS product_authors (
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES authors(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL DEFAULT 'AUTHOR',
    display_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (product_id, author_id, role)
);

CREATE INDEX IF NOT EXISTS idx_product_authors_author_id ON product_authors(author_id);
CREATE INDEX IF NOT EXISTS idx_product_authors_tenant_id ON product_authors(tenant_id);

ALTER TABLE product_authors ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_authors FORCE ROW LEVEL SECURITY;

CREATE POLICY product_authors_tenant_isolation_policy ON product_authors
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 6. SKUs / Variações (Itens com código de barras, ISBN, dimensões, preços e dados fiscais)
CREATE TABLE IF NOT EXISTS skus (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku_code VARCHAR(100) NOT NULL,
    barcode VARCHAR(50),
    isbn VARCHAR(20),
    binding_type VARCHAR(50) DEFAULT 'BROCHURA',
    edition_year INT,
    volume VARCHAR(50),
    pages INT,
    cataloging_sheet_text TEXT,
    weight_grams INT NOT NULL DEFAULT 0,
    dimensions_json JSONB NOT NULL DEFAULT '{"height": 0, "width": 0, "depth": 0}'::jsonb,
    price NUMERIC(15, 2) NOT NULL,
    promotional_price NUMERIC(15, 2),
    cost_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    ncm VARCHAR(20) DEFAULT '4901.99.00',
    cfop VARCHAR(10) DEFAULT '5102',
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sku_tenant_code UNIQUE (tenant_id, sku_code)
);

CREATE INDEX IF NOT EXISTS idx_skus_tenant_id ON skus(tenant_id);
CREATE INDEX IF NOT EXISTS idx_skus_product_id ON skus(product_id);
CREATE INDEX IF NOT EXISTS idx_skus_barcode ON skus(barcode);
CREATE INDEX IF NOT EXISTS idx_skus_isbn ON skus(isbn);

ALTER TABLE skus ENABLE ROW LEVEL SECURITY;
ALTER TABLE skus FORCE ROW LEVEL SECURITY;

CREATE POLICY skus_tenant_isolation_policy ON skus
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 7. Itens Componentes de Kits e Combos (Bundles / Produtos Compostos)
CREATE TABLE IF NOT EXISTS bundle_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    bundle_sku_id UUID NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    item_sku_id UUID NOT NULL REFERENCES skus(id) ON DELETE RESTRICT,
    quantity INT NOT NULL DEFAULT 1,
    bundle_type VARCHAR(50) NOT NULL DEFAULT 'DYNAMIC_VIRTUAL',
    discount_percentage NUMERIC(5, 2) DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_bundle_skus UNIQUE (bundle_sku_id, item_sku_id)
);

CREATE INDEX IF NOT EXISTS idx_bundle_items_tenant_id ON bundle_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_bundle_items_bundle_sku ON bundle_items(bundle_sku_id);
CREATE INDEX IF NOT EXISTS idx_bundle_items_item_sku ON bundle_items(item_sku_id);

ALTER TABLE bundle_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE bundle_items FORCE ROW LEVEL SECURITY;

CREATE POLICY bundle_items_tenant_isolation_policy ON bundle_items
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 8. Metadados de E-book (para download seguro via Presigned URLs)
CREATE TABLE IF NOT EXISTS ebook_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL UNIQUE REFERENCES skus(id) ON DELETE CASCADE,
    file_s3_key VARCHAR(500) NOT NULL,
    file_format VARCHAR(20) NOT NULL DEFAULT 'EPUB',
    file_size_bytes BIGINT NOT NULL DEFAULT 0,
    drm_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ebook_metadata_tenant_id ON ebook_metadata(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ebook_metadata_sku_id ON ebook_metadata(sku_id);

ALTER TABLE ebook_metadata ENABLE ROW LEVEL SECURITY;
ALTER TABLE ebook_metadata FORCE ROW LEVEL SECURITY;

CREATE POLICY ebook_metadata_tenant_isolation_policy ON ebook_metadata
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 9. Imagens e Mídias dos Produtos
CREATE TABLE IF NOT EXISTS product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku_id UUID REFERENCES skus(id) ON DELETE CASCADE,
    image_url VARCHAR(1000) NOT NULL,
    s3_key VARCHAR(500),
    is_primary BOOLEAN NOT NULL DEFAULT false,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_images_tenant_id ON product_images(tenant_id);
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_sku_id ON product_images(sku_id);

ALTER TABLE product_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE product_images FORCE ROW LEVEL SECURITY;

CREATE POLICY product_images_tenant_isolation_policy ON product_images
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

-- 10. Saldo de Estoque por Filial / Empresa (Base Operacional)
CREATE TABLE IF NOT EXISTS stock_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES skus(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    min_threshold INT NOT NULL DEFAULT 0,
    location_ref VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stock_tenant_company_sku UNIQUE (tenant_id, company_id, sku_id)
);

CREATE INDEX IF NOT EXISTS idx_stock_items_tenant_id ON stock_items(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_company_id ON stock_items(company_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_sku_id ON stock_items(sku_id);

ALTER TABLE stock_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE stock_items FORCE ROW LEVEL SECURITY;

CREATE POLICY stock_items_tenant_isolation_policy ON stock_items
    AS PERMISSIVE FOR ALL
    USING (is_bypass_rls() OR tenant_id = current_app_tenant())
    WITH CHECK (is_bypass_rls() OR tenant_id = current_app_tenant());

