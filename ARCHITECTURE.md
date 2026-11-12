# Especificação Arquitetural e de Engenharia de Software

Este documento consolida a arquitetura técnica, modelagem de dados, diagramas UML em Mermaid, catálogo de endpoints e fluxos operacionais da **Plataforma de E-commerce, PDV e Gestão Multi-Tenant**.

Para a documentação web interativa com renderização dinâmica, acesse o endpoint:
👉 `http://localhost:8080/docs` (ou `/documentation`)

---

## 1. Visão Geral da Arquitetura (3 Camadas)

A plataforma substitui o ecossistema monolítico legado WordPress/WooCommerce por uma arquitetura moderna, desacoplada (Headless), orientada a eventos e baseada nos princípios da **Clean Architecture (Hexagonal)** com suporte nativo a **Virtual Threads (Project Loom)** no Java 21 LTS e isolamento estrito de múltiplos lojistas via **Row-Level Security (RLS)** no PostgreSQL 16.

```mermaid
flowchart TB
    subgraph Frontend["1. Camada Frontend (React 19 / Next.js)"]
        Loja["Loja Pública (Next.js SSR / App Router)"]
        PDV["PDV Mobile PWA (Barcode / Câmera ZXing)"]
        Admin["Painel Administrativo SPA (Gestão & Feiras)"]
    end

    subgraph Traefik["Proxy Reverso & SSL (Portátil VPS)"]
        Trf["Traefik v3 (Let's Encrypt / Auto-SSL)"]
    end

    subgraph Backend["2. Camada Backend (Java 21 LTS / Spring Boot 3.3.5)"]
        direction TB
        Filtros["Filtros HTTP (TenantFilter, MdcLoggingFilter, JwtFilter)"]
        API["REST Controllers (Inbound Adapters)"]
        UseCases["Inbound Ports & Application Services (Loom Threads)"]
        Domain["Domain Core (Entities, Enums, Validators, Fiscal Rateio)"]
        OutPorts["Outbound Ports (Repository, Storage, Messaging)"]
        Adapters["Outbound Adapters (JPA, Redis, RabbitMQ, S3)"]

        Filtros --> API --> UseCases --> Domain
        UseCases --> OutPorts --> Adapters
    end

    subgraph Storage["3. Armazenamento & Mensageria"]
        PG[("PostgreSQL 16 (RLS Ativado)")]
        RD[("Redis 7 (Tokens & Cache)")]
        RMQ[("RabbitMQ 3 (Filas Fiscais)")]
        S3[("MinIO S3 (Capas & E-books)")]
    end

    Frontend -->|HTTPS| Trf
    Trf -->|Reverse Proxy| Backend
    Adapters -->|SET LOCAL app.current_tenant| PG
    Adapters -->|Blacklist & Tokens| RD
    Adapters -->|Async Jobs| RMQ
    Adapters -->|Presigned URLs| S3
```

---

## 2. Diagrama de Classes de Domínio (UML Class Diagram)

O domínio da aplicação é desacoplado de anotações de persistência ou de frameworks (DDD/Clean Architecture).

```mermaid
classDiagram
    class Tenant {
        +UUID id
        +String slug
        +String name
        +String cnpj
        +TenantStatus status
        +Map settings
    }

    class Company {
        +UUID id
        +UUID tenantId
        +String code
        +String name
        +TaxRegime taxRegime
        +boolean headquarters
        +Address address
    }

    class User {
        +UUID id
        +UUID tenantId
        +String email
        +String passwordHash
        +UserRole role
        +List~UUID~ allowedCompanyIds
        +boolean hasCompanyAccess(UUID companyId)
    }

    class Product {
        +UUID id
        +UUID tenantId
        +UUID categoryId
        +UUID publisherId
        +String title
        +String slug
        +ProductType productType
        +boolean active
        +List~ProductAuthor~ authors
        +List~Sku~ skus
        +addSku(Sku sku)
        +removeSku(UUID skuId)
        +addAuthor(UUID authorId, AuthorRole role)
        +getPrimarySku() Sku
    }

    class Sku {
        +UUID id
        +UUID tenantId
        +UUID productId
        +String skuCode
        +String barcode
        +String isbn
        +BindingType bindingType
        +int weightGrams
        +Dimensions dimensions
        +BigDecimal price
        +BigDecimal promotionalPrice
        +String ncm
        +String cfop
        +List~BundleItem~ bundleItems
        +EbookMetadata ebookMetadata
        +boolean isBundle()
        +boolean isEbook()
        +calculateVirtualStock(Map stockMap) int
        +decomposeBundleForFiscal(BigDecimal invoicedTotal) List~FiscalDecompositionItem~
    }

    class BundleItem {
        +UUID id
        +UUID bundleSkuId
        +UUID itemSkuId
        +int quantity
        +BundleType bundleType
        +BigDecimal discountPercentage
    }

    class Dimensions {
        +double height
        +double width
        +double depth
        +cubicVolume() double
    }

    class IsbnValidator {
        +clean(String isbn)$ String
        +isValid(String isbn)$ boolean
    }

    class FiscalDecompositionItem {
        +UUID skuId
        +String skuCode
        +int quantity
        +BigDecimal originalUnitPrice
        +BigDecimal proportionalUnitPrice
        +BigDecimal proportionalTotal
        +String ncm
        +String cfop
    }

    Tenant "1" --> "*" Company : possui filiais
    Tenant "1" --> "*" User : possui colaboradores
    Tenant "1" --> "*" Product : cataloga
    Product "1" --> "*" Sku : variações
    Sku "1" --> "*" BundleItem : itens compostos
    Sku "1" --> "1" Dimensions : medidas frete
    Sku ..> FiscalDecompositionItem : decompõe em
    Sku ..> IsbnValidator : valida
```

---

## 3. Diagrama Entidade-Relacionamento (ER) & Row-Level Security

Todas as tabelas do catálogo e cadastros transacionais utilizam isolamento multitenant forçado (`FORCE ROW LEVEL SECURITY`). O acesso direto aos dados sem definir a variável de sessão `app.current_tenant` resulta em retorno vazio.

```mermaid
erDiagram
    tenants ||--o{ tenant_domains : "possui dominios"
    tenants ||--o{ companies : "possui filiais"
    tenants ||--o{ users : "possui usuarios"
    companies ||--o{ user_companies : "vincula"
    users ||--o{ user_companies : "pertence a"
    roles ||--o{ role_permissions : "contem permissoes"
    permissions ||--o{ role_permissions : "atribuidas a"

    tenants ||--o{ categories : "RLS protegido"
    tenants ||--o{ authors : "RLS protegido"
    tenants ||--o{ publishers : "RLS protegido"
    tenants ||--o{ products : "RLS protegido"
    categories ||--o{ products : "classifica"
    publishers ||--o{ products : "publica"
    products ||--o{ product_authors : "autoria"
    authors ||--o{ product_authors : "escreveu"

    products ||--o{ skus : "possui variacoes"
    skus ||--o{ bundle_items : "itens do kit"
    skus ||--o| ebook_metadata : "arquivo digital"
    skus ||--o{ product_images : "fotos"
    companies ||--o{ stock_items : "saldo fisico"
    skus ||--o{ stock_items : "estoque por filial"

    tenants {
        uuid id PK
        varchar slug UK
        varchar name
        varchar cnpj UK
        varchar status
        jsonb settings_json
    }

    companies {
        uuid id PK
        uuid tenant_id FK
        varchar code
        varchar name
        varchar tax_regime
        boolean is_headquarters
    }

    products {
        uuid id PK
        uuid tenant_id FK
        uuid category_id FK
        uuid publisherId FK
        varchar title
        varchar slug UK
        varchar product_type
        boolean active
    }

    skus {
        uuid id PK
        uuid tenant_id FK
        uuid product_id FK
        varchar sku_code UK
        varchar barcode
        varchar isbn
        varchar binding_type
        numeric price
        numeric cost_price
        varchar ncm
        varchar cfop
    }

    bundle_items {
        uuid id PK
        uuid tenant_id FK
        uuid bundle_sku_id FK
        uuid item_sku_id FK
        int quantity
        varchar bundle_type
    }

    stock_items {
        uuid id PK
        uuid tenant_id FK
        uuid company_id FK
        uuid sku_id FK
        int quantity
        int reserved_quantity
    }
```

---

## 4. Diagramas de Sequência dos Fluxos Principais

### 4.1. Resolução de Multi-Tenancy e Injeção de RLS no HikariCP

Garante a segurança de isolamento antes de qualquer consulta SQL.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Cliente / React Frontend
    participant Filter as TenantFilter & MdcFilter
    participant Context as TenantContext (ThreadLocal)
    participant DataSource as TenantAwareDataSource
    participant DB as PostgreSQL 16 (RLS Kernel)

    Client->>Filter: Requisição HTTP (Header X-Tenant-ID / Subdomínio / JWT)
    Filter->>Context: setTenantId(uuid)
    Filter->>Filter: MDC.put("tenant_id", uuid)
    Filter->>DataSource: Executa consulta JPA / SQL
    DataSource->>DB: Obter conexão do HikariPool
    DataSource->>DB: SELECT set_config('app.current_tenant', :tenantId, false)
    DataSource->>DB: Executa SELECT / INSERT / UPDATE
    DB-->>DB: Filtra automaticamente via POLICY: USING (tenant_id = current_app_tenant())
    DB-->>DataSource: Retorna apenas registros pertencentes ao Tenant ativo
    DataSource-->>Client: Resposta isolada e segura
```

---

### 4.2. Autenticação JWT, Resolução RBAC e Rotação de Tokens via Redis

```mermaid
sequenceDiagram
    autonumber
    actor User as Operador / Administrador
    participant AuthCtrl as AuthController
    participant AuthService as AuthService
    participant UserRepo as UserRepositoryPort
    participant Redis as Redis 7 Cache
    participant JWT as JwtTokenProvider

    User->>AuthCtrl: POST /api/v1/auth/login {tenantId, email, password}
    AuthCtrl->>AuthService: authenticate(tenantId, email, password)
    AuthService->>UserRepo: findByEmail(tenantId, email)
    UserRepo-->>AuthService: User com PasswordHash BCrypt e Roles
    AuthService->>AuthService: BCrypt.checkpw(password, hash)
    AuthService->>JWT: generateAccessToken(user, authorities)
    AuthService->>JWT: generateRefreshToken(user)
    AuthService->>Redis: Salva RefreshToken com TTL (7 dias)
    AuthService-->>AuthCtrl: AuthResponse {accessToken, refreshToken}
    AuthCtrl-->>User: 200 OK + Tokens

    Note over User,Redis: Rotação de Token (Refresh Token Rotation)
    User->>AuthCtrl: POST /api/v1/auth/refresh {refreshToken}
    AuthCtrl->>AuthService: refreshToken(token)
    AuthService->>Redis: Verifica existência e invalida token anterior
    AuthService->>JWT: Gera novo par Access + Refresh Token
    AuthService->>Redis: Registra novo Refresh Token
    AuthService-->>User: 200 OK + Novo Par de Tokens
```

---

### 4.3. Estoque Virtual Dinâmico para Kits & Combos

A disponibilidade é calculada sob demanda com base no menor saldo disponível entre os itens constituintes:

$$\text{Saldo Virtual} = \min_{i \in \text{Itens}} \left( \left\lfloor \frac{\text{Saldo Disponível}_i}{\text{Quantidade Necessária}_i} \right\rfloor \right)$$

```mermaid
sequenceDiagram
    autonumber
    actor Caixa as PDV Balcão / Loja Online
    participant ProdCtrl as ProductController
    participant CatalogService as CatalogService
    participant SkuRepo as SkuRepositoryPort
    participant StockRepo as StockRepositoryPort

    Caixa->>ProdCtrl: GET /api/v1/products/skus/{skuId}/stock?companyId={filialId}
    ProdCtrl->>CatalogService: calculateAvailableStock(tenantId, companyId, skuId)
    CatalogService->>SkuRepo: findById(skuId)
    SkuRepo-->>CatalogService: Sku (isBundle = true, BundleItems = [ItemA(qty=2), ItemB(qty=1)])
    CatalogService->>StockRepo: findAvailableStockBySkuIds(filialId, [ItemA, ItemB])
    StockRepo-->>CatalogService: Map {ItemA: 10 unid, ItemB: 4 unid}
    CatalogService->>CatalogService: sku.calculateVirtualStock(): min(10/2=5, 4/1=4) = 4
    CatalogService-->>ProdCtrl: Retorna saldo disponível = 4 kits
    ProdCtrl-->>Caixa: 200 OK: 4
```

---

### 4.4. Motor de Rateio e Decomposição Fiscal de Kits para NF-e/NFC-e

Calcula a proporção exata de cada item no preço final faturado do combo, preservando NCM e CFOP e reconciliando centavos de dízimas periódicas:

```mermaid
sequenceDiagram
    autonumber
    actor Fiscal as Motor de Faturamento (SEFAZ)
    participant Ctrl as ProductController
    participant Service as CatalogService
    participant Sku as Sku Domain Model

    Fiscal->>Ctrl: GET /api/v1/products/skus/{skuId}/fiscal-decomposition?invoicedTotal=180.00
    Ctrl->>Service: decomposeBundle(skuId, 180.00)
    Service->>Sku: decomposeBundleForFiscal(180.00)
    Note over Sku: Original A: R$ 89,90 | Original B: R$ 129,90<br/>Fator = 180,00 / (89,90 + 129,90)<br/>Pondera proporcional de centavos e reconcilia sobra na última linha
    Sku-->>Service: List de FiscalDecompositionItem:<br/>[Item A: R$ 73,62 (NCM 4901.99.00), Item B: R$ 106,38 (NCM 4901.99.00)]
    Service-->>Ctrl: Lista de itens decompostos
    Ctrl-->>Fiscal: 200 OK (Soma fecha perfeitamente em R$ 180,00)
```

---

### 4.5. Importação em Lote via Streaming com Rollback Isolado

Suporta arquivos de até 50.000 livros em chunks de 500 registros utilizando Apache POI e Commons CSV:

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Gestor da Livraria
    participant Ctrl as ProductController
    participant Batch as BatchImportService
    participant Parser as Apache POI / Commons CSV Streaming
    participant Tx as TransactionTemplate (Rollback Isolado)
    participant DB as Banco de Dados

    Admin->>Ctrl: POST /api/v1/products/import (Multipart File XLSX/CSV)
    Ctrl->>Batch: importSpreadsheet(tenantId, companyId, inputStream, fileName)
    Batch->>Parser: Inicia leitura em streaming linha a linha
    Parser-->>Batch: Agrupa em chunks de 500 registros
    loop Para cada linha do chunk
        Batch->>Tx: execute(status -> importaRegistro)
        alt Linha Válida
            Tx->>DB: Salva Categoria, Autor, Editora, Produto, SKU e Saldo
            Tx-->>Batch: Sucesso (successCount++)
        else Linha com Erro (ex: ISBN Inválido)
            Tx->>DB: Executa Rollback apenas desta linha
            Tx-->>Batch: Registra erro em List<BatchImportRowError> (failureCount++)
        end
    end
    Batch-->>Ctrl: BatchImportResult {total: 1000, sucesso: 998, falhas: 2, errors: [...]}
    Ctrl-->>Admin: 200 OK + Relatório de Auditoria
```

---

### 4.6. Upload e Download Seguro via Presigned URLs (MinIO S3 / AWS S3)

```mermaid
sequenceDiagram
    autonumber
    actor User as Usuário / Cliente
    participant Backend as Backend Java (Spring Boot)
    participant Storage as S3StorageAdapter (S3Presigner)
    participant S3 as MinIO S3 / Amazon S3

    Note over User,S3: Fluxo de Upload Direto de Capa ou E-book
    User->>Backend: POST /api/v1/products/{id}/upload-cover-url?fileName=capa.jpg
    Backend->>Storage: generatePresignedUploadUrl(bucket, key, 15 min)
    Storage-->>Backend: URL assinada com SigV4
    Backend-->>User: 200 OK {uploadUrl, s3Key, expiresInSeconds: 900}
    User->>S3: PUT [uploadUrl] (Binário da imagem direto ao Storage)
    S3-->>User: 200 OK (Sem sobrecarga de banda no backend)

    Note over User,S3: Fluxo de Download Seguro de E-book Autorizado
    User->>Backend: GET /api/v1/products/skus/{skuId}/download-ebook-url
    Backend->>Backend: Valida compra autorizada do cliente
    Backend->>Storage: generatePresignedDownloadUrl(bucket, key, 15 min)
    Storage-->>Backend: URL assinada temporária
    Backend-->>User: Retorna link temporário para download
    User->>S3: GET [downloadUrl] (Baixa arquivo EPUB/PDF)
```

---

## 5. Catálogo Completo de Endpoints REST

| Método | Endpoint | Permissão RBAC | Descrição |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/health` | Pública | Verificação de integridade e Virtual Threads ativas |
| **GET** | `/docs` | Pública | Portal HTML interativo de documentação e diagramas UML |
| **GET** | `/api/v1/auth/tenant-info` | Pública | Resolução visual do Tenant para a tela de login |
| **POST** | `/api/v1/auth/login` | Pública | Autenticação com retorno de Access e Refresh Tokens |
| **POST** | `/api/v1/auth/refresh` | Pública | Rotação de Refresh Token via Redis |
| **POST** | `/api/v1/auth/logout` | Autenticado | Invalidação e inclusão de token na blacklist do Redis |
| **POST** | `/api/v1/tenants` | `SUPER_ADMIN` | Cadastro de novo Tenant |
| **GET** | `/api/v1/tenants/{id}` | `SUPER_ADMIN` | Buscar Tenant por ID |
| **GET** | `/api/v1/tenants/slug/{slug}` | `SUPER_ADMIN` | Buscar Tenant pelo slug |
| **POST** | `/api/v1/companies` | `TENANT_ADMIN` | Cadastrar filial ou stand de feira |
| **GET** | `/api/v1/companies/{id}` | Autenticado | Buscar filial por ID |
| **GET** | `/api/v1/companies/code/{code}` | Autenticado | Buscar filial por código (ex: MATRIZ) |
| **GET** | `/api/v1/companies` | Autenticado | Listar todas as filiais do Tenant |
| **POST** | `/api/v1/users` | `USER_MANAGE` | Criar usuário e atribuir permissões e filiais |
| **GET** | `/api/v1/users/me` | Autenticado | Consultar perfil e authorities do usuário logado |
| **GET** | `/api/v1/users/{id}` | `USER_MANAGE` | Buscar colaborador por ID |
| **PUT** | `/api/v1/users/{id}` | `USER_MANAGE` | Atualizar dados do colaborador |
| **PUT** | `/api/v1/users/{id}/password` | `USER_MANAGE` | Redefinição de senha com BCrypt |
| **PATCH**| `/api/v1/users/{id}/status` | `USER_MANAGE` | Bloquear ou desbloquear colaborador |
| **POST** | `/api/v1/taxonomy/categories` | `CATALOG_WRITE`| Criar categoria de produto |
| **GET** | `/api/v1/taxonomy/categories` | Pública | Listar categorias do Tenant |
| **POST** | `/api/v1/taxonomy/authors` | `CATALOG_WRITE`| Cadastrar autor |
| **GET** | `/api/v1/taxonomy/authors` | Pública | Listar autores |
| **POST** | `/api/v1/taxonomy/publishers` | `CATALOG_WRITE`| Cadastrar editora |
| **GET** | `/api/v1/taxonomy/publishers` | Pública | Listar editoras |
| **POST** | `/api/v1/products` | `CATALOG_WRITE`| Criar Produto Pai com SKUs iniciais opcionais |
| **PUT** | `/api/v1/products/{id}` | `CATALOG_WRITE`| Atualizar metadados do Produto Pai |
| **GET** | `/api/v1/products/{id}` | Pública | Buscar produto completo por ID |
| **GET** | `/api/v1/products/slug/{slug}` | Pública | Buscar produto pelo slug amigável (Loja Pública) |
| **GET** | `/api/v1/products` | Pública | Pesquisar produtos com filtros facetados e paginação |
| **DELETE**| `/api/v1/products/{id}` | `CATALOG_WRITE`| Remover produto e SKUs |
| **POST** | `/api/v1/products/{id}/skus` | `CATALOG_WRITE`| Adicionar SKU/Variação ao produto |
| **PUT** | `/api/v1/products/skus/{skuId}`| `CATALOG_WRITE`| Atualizar variação |
| **GET** | `/api/v1/products/skus/{skuId}`| Pública | Buscar SKU por ID |
| **GET** | `/api/v1/products/skus/barcode/{barcode}`| Pública | Buscar SKU por código de barras (EAN-13) |
| **GET** | `/api/v1/products/skus/isbn/{isbn}`| Pública | Buscar SKU por ISBN (ISBN-10 ou ISBN-13) |
| **DELETE**| `/api/v1/products/skus/{skuId}`| `CATALOG_WRITE`| Remover variação |
| **GET** | `/api/v1/products/skus/{skuId}/stock`| Pública | Saldo de estoque (calcula estoque virtual para kits) |
| **GET** | `/api/v1/products/skus/{skuId}/fiscal-decomposition`| Pública | Decomposição e rateio proporcional de kit para NF-e/NFC-e |
| **POST** | `/api/v1/products/{id}/upload-cover-url`| `CATALOG_WRITE`| Gerar presigned URL no S3 para upload de capa |
| **POST** | `/api/v1/products/skus/{skuId}/upload-ebook-url`| `CATALOG_WRITE`| Gerar presigned URL no S3 para upload de e-book |
| **GET** | `/api/v1/products/skus/{skuId}/download-ebook-url`| Autenticado | Link temporário de 15 minutos para download de e-book |
| **POST** | `/api/v1/products/import` | `CATALOG_WRITE`| Importação em streaming de XLSX/CSV com rollback isolado |

---

## 6. Portabilidade Twelve-Factor e Deploy

O backend é agnóstico a provedor e permite deploy direto via Docker Compose ou migração elástica para Amazon Web Services:

```bash
# Execução local / Desenvolvimento
docker compose up -d

# Execução em VPS de Produção (com Traefik e Let's Encrypt)
docker compose -f docker-compose.vps.yml up -d

# Rotina de Backup Automatizado do PostgreSQL (com rotação de 7 dias)
./scripts/backup-postgres.sh
```

