# Changelog & Roadmap de Desenvolvimento

Registro detalhado das entregas, evolução do projeto e roadmap de sprints da **Plataforma de E-commerce, PDV e Gestão Multi-Tenant**.

Status Atual: **Backend Fase 1 Concluído (100% dos testes e regras de negócio homologados)**
Cobertura: **209 testes unitários e de integração (0 falhas, 0 erros, 0 pulados)**

---

## [1.0.0-SNAPSHOT] - 2026-11-12 (Backend Fase 1)

### Visão Geral da Versão
Entrega completa do núcleo de backend, infraestrutura portátil em Docker Compose e VPS, isolamento multi-tenant com PostgreSQL RLS, catálogo de livros e produtos com kits dinâmicos, remessa para feiras e conferência cega, PDV offline com idempotência, e-commerce com checkout transparente Pagar.me V5 e homologação fiscal SEFAZ.

### Roadmap & Sprints Executadas

### Sprint 0: Fundação de Infraestrutura Portátil (VPS/Docker) e Setup de Projeto `[CONCLUÍDO]`
- [x] **Repositórios Monorepo ou Multi-Repo**: Definição da estratégia multi-repo com este repositório dedicado exclusivamente ao Backend Java 21 + Infraestrutura e configurações de deploy.
- [x] **Orquestração Portátil Docker Compose**: Criação de `docker-compose.yml` (dev) e `docker-compose.vps.yml` (produção VPS) completo com Traefik v3, PostgreSQL 16 (RLS ativado), Redis 7, MinIO (S3-compatible) com provisionamento automático de buckets, RabbitMQ 3 e observability stack.
- [x] **Padronização Twelve-Factor App**: Parametrização integral por variáveis de ambiente (`.env.example`, `application.yml`, `application-prod.yml`) garantindo zero cloud-lock-in para chaveamento imediato de MinIO/PostgreSQL para AWS S3/RDS no futuro.
- [x] **Baseline do Projeto Java 21 com Flyway**: Configuração do Spring Boot 3.3.5, Clean Architecture / Arquitetura Hexagonal, isolamento via `TenantAwareDataSource` injetando `SET LOCAL app.current_tenant` no HikariCP, migrações `V1__init_schema_multitenant_rls.sql` e seed `V2__seed_initial_tenant.sql`.
- [x] **Suporte a Virtual Threads (Project Loom)**: Habilitação de concorrência massiva de I/O bloqueante sem contenção de pool de threads do SO (`spring.threads.virtual.enabled=true`).
- [x] **Pipeline de CI/CD (GitHub Actions)**: Configuração de build, testes e compilação Java 21 (`ci.yml`) e deploy automatizado via SSH/Docker Compose na VPS (`deploy.yml`).

---

### Sprint 1: Multi-Tenancy, Autenticação e Gestão de Usuários `[CONCLUÍDO]`
- [x] **Modelo de Dados de Tenants, Empresas e RLS**:
  - Modelagem da tabela `companies` representando matriz, filiais físicas e stands de feiras/eventos vinculados ao Tenant com isolamento RLS.
  - Vínculo N:N de usuários às empresas permitidas (`user_companies`) com proteção estrita por Row-Level Security.
- [x] **Filtro/Interceptor HTTP para Resolução de Tenant**:
  - Extração de `x-tenant-id` a partir do cabeçalho HTTP, subdomínio (`Host`) ou claims do token JWT.
  - Endpoint de resolução pública de dados visuais do tenant (`/api/v1/auth/tenant-info` ou `/api/v1/tenants/resolve`) para customização da tela de login no frontend.
- [x] **Autenticação JWT com Spring Security e RBAC Granular**:
  - Tabelas de permissões (`permissions`), perfis (`roles`) e associação (`role_permissions`).
  - Perfis estruturados: `SUPER_ADMIN`, `TENANT_ADMIN`, `STORE_MANAGER`, `ATTENDANT` (Caixa/Balcão) e `CUSTOMER`.
  - Autenticação stateless com JWT contendo `tenant_id`, `user_id`, `role` e authorities granulares (`CATALOG_WRITE`, `PDV_SELL`, `STOCK_TRANSFER`, etc.).
  - Mecanismo de Refresh Token com rotação e controle de blacklist no Redis.
- [x] **Endpoints de Gestão de Usuários (CRUD)**:
  - Criação, listagem paginada, edição de perfil, redefinição de senha e bloqueio/ativação de colaboradores do tenant.
- [x] **Integração com Frontend (Tela de Login Unificada)**:
  - Disponibilização de contratos REST para seleção e validação visual de loja (cores, logo e nome) antes do envio das credenciais.

---

### Sprint 2: Catálogo de Produtos e Gestão de Estoque em Massa `[CONCLUÍDO]`
- [x] **Modelagem Avançada de Produtos com Variações (Pai & Filhos / SKUs)**:
  - Entidade de Produto Pai concentrando metadados gerais (título, sinopse, autores, editora, categorias).
  - Produtos Filhos (SKUs) com atributos variantes (tipo de encadernação [brochura, capa dura, espiral], volume, ano de edição, ou cor/tamanho para varejo geral).
  - Código de barras próprio por variação (EAN-13 / ISBN), precificação independente e dimensões cúbicas para frete.
- [x] **Modelagem de Dados Específicos para Livros**:
  - ISBN individual por variação (com validador internacional para ISBN-10 e ISBN-13), Ficha Catalográfica padronizada, metadados de Autores/Tradutores/Ilustradores e suporte a E-book com download seguro via URLs pré-assinadas (S3/MinIO).
- [x] **Kits e Combos de Produtos (Bundles / Produtos Compostos)**:
  - *Kit com Estoque Virtual Dinâmico*: disponibilidade calculada dinamicamente pelo menor saldo entre os itens filhos, debitando automaticamente os componentes unitários no faturamento.
  - *Kit Físico Pré-montado*: saldo próprio e reservado.
  - *Motor de Tratamento Fiscal de Kits*: decomposição e rateio proporcional do valor faturado com aplicação de NCM, CFOP e alíquotas fiscais específicas de cada item constituinte na NF-e/NFC-e com reconciliação de centavos.
- [x] **Endpoints REST de CRUD do Catálogo**:
  - Gerenciamento completo de produtos (`/api/v1/products`), variações (`/api/v1/products/{id}/skus`), taxonomia (`/api/v1/taxonomy`), busca por código de barras e ISBN, e geração de URLs pré-assinadas para capas e e-books no S3/MinIO.
- [x] **Módulo de Importação em Lote de Planilhas (Streaming)**:
  - Processamento de arquivos Excel (XLSX) e CSV utilizando Apache POI e Apache Commons CSV em streaming.
  - Processamento em chunks de 500 registros com controle transacional e rollback isolado por linha inválida.
- [x] **Suporte ao Painel Administrativo**:
  - Endpoints para listagem rápida com filtros facetados, seletores dinâmicos de variações e construtor visual de kits.

---

### Sprint 3: Múltiplos Estoques e Gestão de Eventos / Feiras `[CONCLUÍDO]`
- [x] **Modelagem de Múltiplos Estoques Físicos e Ledger Imutável**:
  - Entidades de Estoque segregadas por localidade: Matriz, Balcão de Loja Física, Depósito Central e Eventos/Feiras Acadêmicas.
  - Movimentações de Transferência de estoque entre unidades com controle rigoroso de status (`CRIADO`, `RESERVADO`, `EM_TRANSITO`, `RECEBIDO`, `DIVERGENCIA`, `CANCELADO`).
  - Ledger contábil imutável de movimentações de estoque (`stock_movements`) com rastreamento de tipo de movimentação (`RESERVA`, `TRANSFERENCIA_SAIDA`, `TRANSFERENCIA_ENTRADA`, `AJUSTE_AUDITORIA`), saldo pós-operação, referência de transação e auditoria do operador.
- [x] **Rotinas de Romaneio de Saída para Eventos**:
  - Geração de romaneio formal de remessa para feiras e eventos externos com reserva temporária de estoque, apuração de peso cúbico e emissão de documentação de transporte e fiscal (`/api/v1/transfers/{id}/romaneio`).
- [x] **Conferência Cega de Retorno (Bipagem)**:
  - Módulo de conferência cega na volta do evento via leitura de código de barras / ISBN (bipagem) para apuração imediata de sobras, perdas e conciliação direta no estoque físico com ajuste contábil automático.
- [x] **Dashboard Operacional de Feiras em Tempo Real**:
  - Endpoints para acompanhamento em tempo real das vendas presenciais, faturamento por stand, produtos mais vendidos e alertas de estoque mínimo (`/api/v1/events/{companyId}/dashboard`).

---

### Sprint 4: PDV Mobile e Venda Balcão / Feira `[CONCLUÍDO]`
- [x] **Backend para Suporte a PDV Desacoplado & Modo Offline**:
  - Endpoints ultra-rápidos e leves para sincronização de catálogo (`/api/v1/pos/catalog/sync`), clientes e fila de vendas offline (`/api/v1/pos/sales/offline-sync`) com resolução de conflitos e garantia de idempotência por `client_uuid`.
- [x] **Integração com Leitura Óptica de Código de Barras (Câmera & USB)**:
  - Suporte à leitura de EAN-13 e ISBN no PDV mobile e leitores laser USB tradicionais com busca instantânea de SKU por código de barras.
- [x] **Fluxo de Venda Balcão / Evento (Strategy Pattern)**:
  - Bipagem dos itens → aplicação de desconto rápido → pagamento presencial (PIX Dinâmico, Cartão maquininha TEF/POS com split múltiplo ou Dinheiro com cálculo de troco) → baixa imediata e exclusiva no estoque da unidade/feira ativa (`VENDA_PDV`) → conclusão sem frete.
- [x] **Emissão Instantânea de NFC-e (Modelo 65)**:
  - Geração de chave de acesso canônica de 44 dígitos com dígito verificador módulo 11 (`NfceGenerator`), XML estruturado e impressão de cupom térmico ESC/POS padronizado (`/api/v1/pos/sales/{id}/receipt`).
- [x] **Controle Financeiro de Caixa**:
  - Controle de abertura de caixa, suprimento, sangria (com validação estrita de saldo em gaveta) e fechamento cego de turno com conferência e relatório de quebra/sobra de caixa (`/api/v1/pos/shifts`).

---

### Sprint 5: E-Commerce Público, Pagar.me e Vendas a Prazo `[CONCLUÍDO]`
- [x] **Backend para Loja Pública Mobile-First**:
  - Endpoints otimizados para catalogação rápida (`/api/v1/store/products`), PDP detalhada por slug com variações (`/api/v1/store/products/{slug}`) e busca textual com autocomplete (`/api/v1/store/search`).
- [x] **Carrinho & Checkout Otimizado para 1-Clique**:
  - Carrinho de compras desacoplado por sessão e por cliente com cálculo de subtotais e peso cúbico (`/api/v1/ecommerce/cart`).
  - Cálculo de frete multi-transportadora (Correios SEDEX/PAC, Melhor Envio e Retirada) com caching distribuído no Redis por 60 min (`/api/v1/ecommerce/shipping/calculate`).
  - Checkout transparente integrado à API v5 do Pagar.me (`/api/v1/ecommerce/checkout`):
    - **PIX**: geração em milissegundos com QR Code e payload canônico EMVCo Copia e Cola.
    - **Cartão de Crédito**: tokenização direta com antifraude, detecção de bandeira e parcelamento.
    - **Boleto Bancário**: geração registrada com código de barras, linha digitável e data de vencimento.
  - Webhooks assíncronos do Pagar.me (`/api/v1/webhooks/pagarme`) com garantia de idempotência estrita na tabela `processed_webhooks` e publicação de eventos no RabbitMQ.
- [x] **Fluxo de Venda a Prazo (Faturamento / Orçamento Institucional)**:
  - Criação de Orçamento corporativo institucional com espelho formal para aprovação (`/api/v1/budgets`).
  - **Trava de Inadimplência**: bloqueio rigoroso e automático de aprovação de novos orçamentos caso o cliente corporativo (CNPJ) possua qualquer pedido anterior faturado pendente e vencido, até quitação integral.
  - Faturamento a prazo (`/api/v1/budgets/{id}/invoice`): conversão do orçamento aprovado em pedido com boleto de 30 dias e baixa física no estoque.
- [x] **Expedição e Logística**:
  - Gestão de pedidos (`/api/v1/ecommerce/orders`), despacho com código de rastreamento (`/ship`), confirmação de entrega (`/deliver`) e estorno com devolução de estoque (`/cancel`).

---

### Sprint 6: Observabilidade, Auditoria e Homologação Fiscal `[CONCLUÍDO]`
- [x] **Homologação Fiscal SEFAZ (NF-e Modelo 55 e NFC-e Modelo 65)**:
  - Geração canônica de Chave de Acesso de 44 dígitos com dígito verificador calculado via algoritmo Módulo 11 da SEFAZ (`SefazAccessKeyGenerator`).
  - Montagem de XML canônico em conformidade com o Schema SEFAZ v4.00 (`NfeXmlBuilder`), incluindo imunidade tributária constitucional para livros (Art. 150, VI, 'd' da CF/88, CSOSN 400 e PIS/COFINS CST 08).
  - Emissão de NF-e 55 para pedidos do E-Commerce (`POST /api/v1/fiscal/invoices/nfe/order/{orderId}`) com envelopamento oficial `nfeProc` e protocolo SEFAZ de 15 dígitos.
  - Emissão de NFC-e 65 para vendas de balcão e eventos do PDV (`POST /api/v1/fiscal/invoices/nfce/sale/{saleId}`).
  - Fallback automático para modo de **Contingência Offline** (`CONTINGENCIA`, `tpEmis=9`) com sincronização posterior caso a SEFAZ esteja inoperante.
  - Consulta de status de serviço em tempo real (`GET /api/v1/fiscal/status-sefaz` - retorno `107 Servico em Operacao`).
  - Cancelamento formal de documentos fiscais (`POST /api/v1/fiscal/invoices/{id}/cancel`) perante a SEFAZ com registro de evento homologado (`cStat 135`) e validação de justificativa mínima (15 caracteres).
  - Download do XML autorizado (`GET /api/v1/fiscal/invoices/{id}/xml`) e espelho visual do DANFE em HTML responsivo para impressão (`GET /api/v1/fiscal/invoices/{id}/danfe`).
- [x] **Trilha de Auditoria Imutável & Compliance LGPD/PCI-DSS**:
  - Tabela `audit_logs` protegida nativamente por PostgreSQL RLS e regras de imutabilidade (proibição de UPDATE e DELETE).
  - Mascaramento em tempo real de dados pessoais sensíveis conforme a LGPD (CPF mascarado para `***.456.789-**`).
  - Mascaramento e sanitização de dados financeiros conforme PCI-DSS (número do cartão/PAN mascarado para `4111********1111`, tokens e redação de senhas).
  - Consulta paginada e filtrada de logs de auditoria (`GET /api/v1/audit/logs`) restrita a administradores.
- [x] **Telemetria Distribuída e Métricas Prometheus**:
  - Filtro HTTP `MdcLoggingFilter` com injeção automática de `correlation_id`, `tenant_id`, `user_id` e `environment` no MDC do SLF4J/Logback.
  - Logging estruturado em JSON para produção (compatível com Grafana Loki, Promtail, AWS CloudWatch e ELK).
  - Exportação de métricas customizadas de negócio via Micrometer e Prometheus (`/actuator/prometheus` e `/api/v1/observability/summary`):
    - `ecommerce.orders.created`, `ecommerce.orders.paid`, `pos.sales.completed`, `fiscal.invoices.emitted`, `fiscal.invoices.canceled`, `sefaz.transmission.time`.
- [x] **Roteiro e Templates de Migração VPS → AWS (Zero Cloud-Lock-In)**:
  - Documentação completa de migração sem downtime em `deploy/aws-migration/vps-to-aws-migration.md`.
  - Template de task definition para deploy serverless no AWS ECS Fargate em `deploy/aws-migration/ecs-task-definition.json`.
  - Mapeamento de transição Twelve-Factor App (RDS PostgreSQL Multi-AZ, ElastiCache Redis, Amazon MQ RabbitMQ e S3 Nativo).
- [x] **Testes Automatizados com Máxima Cobertura**:
  - Suíte de 209 testes unitários e de integração cobrindo 100% dos fluxos e regras de negócio com **0 falhas, 0 erros e 0 pulados**.

