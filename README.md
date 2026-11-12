# Plataforma de E-commerce, PDV e Gestão Multi-Tenant (Backend)

[![CI - Build & Test](https://github.com/usuario/e-commerce-livraria/actions/workflows/ci.yml/badge.svg)](https://github.com/usuario/e-commerce-livraria/actions/workflows/ci.yml)
[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker Compose](https://img.shields.io/badge/Docker%20Compose-v2-blue.svg)](https://docs.docker.com/compose/)
[![Tests Passing](https://img.shields.io/badge/Tests-209%20passed-success.svg)](#8-executando-os-testes)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20Hexagonal-purple.svg)](ARCHITECTURE.md)

Repositório central de **Backend e Infraestrutura Portátil** da nova plataforma de e-commerce, frente de caixa (PDV) e gestão para livrarias e varejo em geral, desenvolvida sob os princípios da **Clean Architecture (Hexagonal)** com **Java 21 LTS**, **Virtual Threads (Project Loom)** e isolamento de dados estrito por **PostgreSQL Row-Level Security (RLS)**.

> O **Frontend** (Next.js SSR para loja virtual e React PWA para PDV/Painel) é mantido em repositório dedicado desacoplado.

---

## 1. Visão Geral do Sistema

A plataforma foi projetada para unificar as operações digitais e presenciais de livrarias e comércios correlatos em uma arquitetura moderna e escalável, atendendo a três frentes principais:

1. **Loja Pública (E-Commerce Headless)**: Vitrine online de alto desempenho, busca textual com autocomplete, carrinho desacoplado, cálculo de frete multi-transportadora e checkout transparente com Pagar.me V5.
2. **Frente de Caixa (PDV Mobile & Feiras)**: Terminal de vendas presencial offline-first para lojas físicas e stands em feiras acadêmicas/bienais, com leitor de código de barras/ISBN, controle de turnos de caixa, troco, split de pagamentos e contingência offline garantida por chaves de idempotência.
3. **Painel de Gestão (Backoffice Multi-Tenant)**: Administração centralizada de produtos e variações (SKUs), construtor de kits virtuais dinâmicos, remessa para feiras e conferência cega pós-evento com ledger contábil de estoque, aprovação de orçamentos institucionais B2B com trava de inadimplência e homologação fiscal SEFAZ (NF-e 55 e NFC-e 65).

---

## 2. Principais Funcionalidades

- **Multi-Tenancy Nativo com PostgreSQL RLS**: Isolamento absoluto entre diferentes lojistas garantido no kernel do banco de dados via políticas com `FORCE ROW LEVEL SECURITY`.
- **Catálogo de Livros & Varejo Geral**: Gestão de Produtos Pai e Variações (SKUs), validação internacional de ISBN-10/13, metadados de autores/editoras e entrega segura de E-books com presigned URLs (S3/MinIO).
- **Kits Virtuais com Rateio Fiscal**: Cálculo dinâmico de saldo de combos pelo menor componente em estoque e decomposição fiscal proporcional para NF-e com reconciliação de centavos.
- **Importação em Lote via Streaming**: Ingestão de planilhas XLSX e CSV em blocos com rollback isolado por linha inválida e relatório de auditoria detalhado.
- **Operações de Feiras & Eventos**: Remessas de estoque com romaneio de transporte, auditoria cega pós-feira com bipagem rápida e conciliação atômica no ledger de movimentações.
- **Frente de Caixa (PDV) & Turnos**: Abertura de turno com fundo de troco, suprimentos, sangrias com trava de saldo em gaveta, fechamento cego de caixa e emissão de cupom térmico ESC/POS.
- **Checkout Transparente Pagar.me V5**: Pagamentos via PIX instantâneo com QR Code dinâmico, Cartão de Crédito e Boleto Bancário com webhooks assíncronos e idempotência estrita.
- **Venda a Prazo B2B com Trava de Inadimplência**: Emissão de orçamentos corporativos com bloqueio automático caso o cliente possua títulos ou pedidos faturados vencidos.
- **Módulo Fiscal SEFAZ Homologado**: Emissão de NF-e (Modelo 55) e NFC-e (Modelo 65), chave de 44 dígitos com algoritmo Módulo 11, contingência offline automática, cancelamento perante SEFAZ e espelho de DANFE em HTML responsivo.
- **Observabilidade & Compliance**: Trilha imutável de auditoria com mascaramento em tempo real (LGPD/PCI-DSS), logs JSON estruturados com MDC e métricas Prometheus integradas ao Micrometer.

---

## 3. Requisitos do Sistema

- **Docker** (versão 24.0 ou superior) e **Docker Compose v2**
- **Java OpenJDK 21 LTS** (para compilação e execução local)
- **Maven 3.8+** (para gerenciamento de dependências e testes)

---

## 4. Como Rodar o Projeto

### Passo a Passo Rápido

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/usuario/e-commerce-livraria.git
   cd e-commerce-livraria
   ```

2. **Configure as variáveis de ambiente:**
   ```bash
   cp .env.example .env
   ```

3. **Suba a infraestrutura completa via Docker Compose:**
   ```bash
   docker compose up -d
   ```

4. **Execute o Backend:**
   - **Opção A (Via Docker Compose):** O container `backend` já sobe automaticamente configurado.
   - **Opção B (Localmente via Maven):**
     ```bash
     mvn spring-boot:run
     ```
     *Ou execute o arquivo JAR pré-compilado:*
     ```bash
     mvn clean package -DskipTests
     java -jar target/ecommerce-backend-1.0.0-SNAPSHOT.jar
     ```

5. **Verifique a integridade da aplicação:**
   ```bash
   curl -X GET http://localhost:8080/api/v1/health
   ```

---

## 5. Serviços e Portas do Ambiente Local

| Serviço | Porta / URL Local | Descrição |
| :--- | :--- | :--- |
| **Backend REST API** | `http://localhost:8080` | Servidor Spring Boot 3.3.5 com Virtual Threads |
| **Documentação Viva & UML** | `http://localhost:8080/documentation` | Portal web interativo com diagramas, zoom/pan e catálogo |
| **Swagger UI (OpenAPI 3)** | `http://localhost:8080/swagger-ui.html` | Console interativo para teste de endpoints REST |
| **OpenAPI Docs (JSON)** | `http://localhost:8080/v3/api-docs` | Especificação completa da API em JSON bruto |
| **PostgreSQL 16 (RLS)** | `localhost:5433` (externa) | Banco de dados relacional com Row-Level Security ativado |
| **Redis 7** | `localhost:6379` | Cache de alta velocidade, blacklist de tokens e sessões |
| **MinIO Console** | `http://localhost:9001` | Armazenamento de objetos S3 local (capas e e-books) |
| **RabbitMQ Management** | `http://localhost:15672` | Painel de controle de filas e mensageria assíncrona |
| **Grafana Dashboards** | `http://localhost:3030` | Interface visual de monitoramento de logs e métricas |
| **Prometheus Server** | `http://localhost:9090` | Coleta e consulta de métricas de performance e negócio |
| **Grafana Loki** | `http://localhost:3100` | Ingestão e agregação de logs estruturados em JSON |
| **Métricas Prometheus (Raw)** | `http://localhost:8080/actuator/prometheus` | Exportação de métricas da JVM e Spring Boot |

---

## 6. Credenciais Padrão (Ambiente de Desenvolvimento / Seed)

### Acesso à API & Tenant Matriz
- **Tenant ID Padrão**: `00000000-0000-0000-0000-000000000001`
- **E-mail**: `admin@livrariamatriz.com.br`
- **Senha**: `Admin@123456`
- **Autenticação via cURL:**
  ```bash
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{
      "tenantId": "00000000-0000-0000-0000-000000000001",
      "email": "admin@livrariamatriz.com.br",
      "password": "Admin@123456"
    }'
  ```

### Painéis de Administração & Monitoramento
- **Grafana (`http://localhost:3030`)**:
  - Usuário: `admin`
  - Senha: `admin_secret_password`
  - *Datasources pré-configurados:* Loki (Logs) e Prometheus (Métricas)
  - *Dashboard automático:* `Livraria Multi-Tenant - Visão Geral & Logs`
- **MinIO Console (`http://localhost:9001`)**:
  - Usuário: `minio_admin_user`
  - Senha: `minio_admin_secret_password`
- **RabbitMQ Management (`http://localhost:15672`)**:
  - Usuário: `rabbit_user`
  - Senha: `rabbit_secret_password`

---

## 7. Monitoramento e Análise de Logs (Grafana + Loki)

A plataforma utiliza **Logstash Logback Encoder** para produzir logs estruturados em JSON enriquecidos com contexto distribuído via MDC (`tenant_id`, `correlation_id`, `user_id`, `environment`). O agente **Promtail** coleta esses logs em tempo real e os envia ao **Grafana Loki**.

Acesse o **Grafana** (`http://localhost:3030`), navegue até **Explore** e selecione o datasource **Loki**:

- **Filtrar todos os logs de um Tenant específico:**
  ```logql
  {container="ecommerce-backend"} | json | tenant_id="00000000-0000-0000-0000-000000000001"
  ```
- **Rastrear uma requisição ponta a ponta pelo Correlation ID:**
  ```logql
  {container="ecommerce-backend"} | json | correlation_id="550e8400-e29b-41d4-a716-446655440000"
  ```
- **Filtrar apenas erros e exceções:**
  ```logql
  {container="ecommerce-backend"} | json | level="ERROR"
  ```
- **Filtrar logs operacionais do PDV e Vendas:**
  ```logql
  {container="ecommerce-backend"} |= "pos" or "checkout"
  ```

---

## 8. Executando os Testes

O projeto conta com uma suíte abrangente de testes unitários e de integração cobrindo todas as regras de domínio, validações fiscais e cenários de persistência:

```bash
mvn test
```

> **Status Atual da Suíte de Testes:**
> - **Total de testes:** 209 testes automatizados
> - **Erros:** 0
> - **Falhas:** 0
> - **Ignorados/Pulados:** 0
> - **Taxa de aprovação:** 100%

---

## 9. Documentação Técnica & Links Úteis

Para consultar especificações técnicas detalhadas, diagramas, endpoints e histórico de desenvolvimento:

- 📐 **[ARCHITECTURE.md](ARCHITECTURE.md)**: Especificação técnica profunda, diagrama de arquitetura em 3 camadas, estrutura hexagonal de pastas, diagramas completos de classes (UML) e entidade-relacionamento (ER), todos os 15 diagramas de sequência dos fluxos operacionais, catálogo de endpoints e guia de deploy Twelve-Factor / AWS.
- 📋 **[CHANGELOG.md](CHANGELOG.md)**: Histórico completo de versões, roadmap detalhado e todas as entregas das Sprints 0 a 6.
- 🌐 **[Portal de Documentação Web Viva](http://localhost:8080/documentation)**: Interface interativa embarcada no backend com visualizador dinâmico de diagramas (pan/zoom), catálogo de rotas e guias visuais.
