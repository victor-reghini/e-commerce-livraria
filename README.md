# Plataforma de E-commerce, PDV e Gestão Multi-Tenant (Backend & Infraestrutura)

[![CI - Build & Test](https://github.com/usuario/e-commerce-livraria/actions/workflows/ci.yml/badge.svg)](https://github.com/usuario/e-commerce-livraria/actions/workflows/ci.yml)
[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker Compose](https://img.shields.io/badge/Docker%20Compose-v2-blue.svg)](https://docs.docker.com/compose/)
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20%2F%20Clean-purple.svg)](#arquitetura-do-backend)

Repositório central de **Backend e Infraestrutura Portátil** da nova plataforma de e-commerce, frente de caixa (PDV) e gestão para livrarias e varejo em geral, substituindo a infraestrutura legada baseada em WordPress/WooCommerce.

> O **Frontend** (Next.js SSR para loja pública e React PWA para PDV/Painel) é mantido em repositório dedicado desacoplado.

---

## 1. Visão Geral da Arquitetura

O sistema adota o padrão **Clean Architecture / Arquitetura Hexagonal (Ports & Adapters)** com **Java 21 LTS** e **Project Loom (Virtual Threads)** ativadas nativamente para altíssima concorrência I/O:

```
src/main/java/com/livraria/ecommerce/
├── domain/                    # Núcleo de regras de negócio puras (sem dependência de frameworks)
│   ├── model/                 # Entidades e Value Objects (Tenant, User, etc.)
│   ├── port/in/               # Use Cases / Portas de Entrada
│   ├── port/out/              # Portas de Saída (Repositórios, Storage, Mensageria)
│   └── exception/             # Exceções de Domínio e Negócio
├── application/               # Serviços de Aplicação e Orquestração
│   ├── service/               # Implementação dos Use Cases
│   └── dto/                   # DTOs de comando e transferência
└── infrastructure/            # Adaptadores de Tecnologia e Frameworks
    ├── adapters/in/web/       # Controllers REST, Filtros (Tenant, MDC Logging) e Exception Handlers
    ├── adapters/out/          # Implementações das Portas de Saída:
    │   ├── persistence/       # PostgreSQL com Row-Level Security (RLS) e HikariCP Tenant Aware
    │   └── storage/           # AWS SDK v2 (S3 nativo e MinIO local compatível)
    ├── config/                # Configurações Spring (Segurança, OpenAPI, RabbitMQ, Redis, DataSource)
    └── security/              # Autenticação JWT, UserPrincipal e Filtros de Segurança
```

### Isolamento Multi-Tenant com PostgreSQL RLS (Row-Level Security)
Diferente de abordagens frágeis onde o isolamento depende exclusivamente de cláusulas `WHERE tenant_id = ?` no código da aplicação, a plataforma utiliza **Row-Level Security (RLS) nativo no PostgreSQL**:
1. O filtro HTTP intercepta a requisição e extrai o `tenant_id` (via cabeçalho `X-Tenant-ID`, subdomínio ou token JWT).
2. O `TenantAwareDataSource` injeta o contexto na conexão do pool HikariCP: `SELECT set_config('app.current_tenant', :tenantId, false)`.
3. O PostgreSQL bloqueia nativamente em nível de engine qualquer tentativa de leitura ou escrita fora do tenant ativo através da política `users_tenant_isolation_policy` com `FORCE ROW LEVEL SECURITY`.

---

## 2. Stack de Infraestrutura & Observabilidade (Fase 1: VPS)

Projetado sob o princípio do **Twelve-Factor App** e **Zero Cloud-Lock-In**, permitindo execução idêntica em máquina local, em VPS Linux com Docker Compose, ou em serviços gerenciados AWS (ECS/RDS/ElastiCache/S3) apenas alterando variáveis de ambiente.

| Serviço | Tecnologia | Papel no Ecossistema |
| :--- | :--- | :--- |
| **Roteamento & SSL** | Traefik v3 | Descoberta automática de containers e emissão de SSL Let's Encrypt |
| **Banco de Dados** | PostgreSQL 16 Alpine | Banco relacional com RLS e extensões `uuid-ossp` e `pgcrypto` |
| **Cache & Sessões** | Redis 7.2 Alpine | Cache de alta velocidade, sessões e rate limiting |
| **Armazenamento S3** | MinIO Server | S3-compatible local com buckets pré-configurados (`ecommerce-media`, `ecommerce-ebooks`) |
| **Mensageria** | RabbitMQ 3.13 Management | Processamento assíncrono de NF-e, webhooks e notificações |
| **Métricas** | Prometheus + Micrometer | Coleta de métricas da JVM, Virtual Threads e latências de checkout |
| **Logs Estruturados** | Grafana Loki + Promtail | Centralização de logs estruturados em JSON com campos de MDC |

---

## 3. Início Rápido (Desenvolvimento Local)

### Pré-requisitos
- [Docker](https://docs.docker.com/engine/install/) e [Docker Compose](https://docs.docker.com/compose/)
- [Java 21 OpenJDK](https://adoptium.net/) (opcional se for executar via Docker)
- [Maven 3.8+](https://maven.apache.org/) (opcional se for executar via Docker)

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/seu-usuario/e-commerce-livraria.git
   cd e-commerce-livraria
   ```

2. **Crie o arquivo de variáveis de ambiente:**
   ```bash
   cp .env.example .env
   ```

3. **Inicie todos os serviços com o Docker Compose:**
   ```bash
   docker compose up -d
   ```

4. **Verifique o status dos serviços:**
   ```bash
   docker compose ps
   ```

5. **Acompanhe os logs da aplicação:**
   ```bash
   docker compose logs -f backend
   ```

---

## 4. Endpoints Principais & Documentação Viva

Após iniciar o container, acesse:

| Recurso | URL Local | Descrição |
| :--- | :--- | :--- |
| **Swagger UI (OpenAPI 3)** | `http://localhost:8080/swagger-ui.html` | Documentação interativa e teste de rotas REST |
| **OpenAPI Docs (JSON)** | `http://localhost:8080/v3/api-docs` | Especificação OpenAPI 3 |
| **Health Check da API** | `http://localhost:8080/api/v1/health` | Estado do backend, ambiente e Virtual Threads |
| **Métricas Prometheus** | `http://localhost:8080/actuator/prometheus` | Métricas de sistema e negócio |
| **Painel MinIO Console** | `http://localhost:9001` | Credenciais: `minio_admin_user` / `minio_admin_secret_password` |
| **Painel RabbitMQ** | `http://localhost:15672` | Credenciais: `rabbit_user` / `rabbit_secret_password` |

### Exemplos de Requisições via cURL

#### 1. Verificar integridade do backend
```bash
curl -X GET http://localhost:8080/api/v1/health
```

#### 2. Autenticar usuário default (Tenant Matriz)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "00000000-0000-0000-0000-000000000001",
    "email": "admin@livrariamatriz.com.br",
    "password": "Admin@123456"
  }'
```

#### 3. Cadastrar um novo Tenant
```bash
curl -X POST http://localhost:8080/api/v1/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <SEU_JWT_TOKEN>" \
  -d '{
    "slug": "livraria-paulista",
    "name": "Livraria Paulista Ltda",
    "tradeName": "Livraria Paulista",
    "cnpj": "98765432000100",
    "email": "contato@livrariapaulista.com.br",
    "phone": "11988887777",
    "primaryDomain": "paulista.meudominio.com.br",
    "settings": {
      "primaryColor": "#ff6600",
      "allowInstallments": true
    }
  }'
```

---

## 5. Deploy em Produção (VPS Linux)

Para implantação em servidor VPS (Ubuntu/Debian), utilize a orquestração `docker-compose.vps.yml`:

```bash
# 1. Configurar variáveis no servidor
cp .env.example .env
nano .env  # Configurar DOMAIN_NAME, TRAEFIK_ACME_EMAIL e senhas seguras

# 2. Inicializar os serviços com Traefik, SSL Let's Encrypt e limites de recursos
docker compose -f docker-compose.vps.yml up -d
```

### Rotina de Backup Automatizado do Banco
O repositório inclui o script [backup-postgres.sh](file:///home/npd/Downloads/projects/e-commerce-livraria/scripts/backup-postgres.sh) com `pg_dump` compactado e rotação automática de 7 dias:
```bash
# Adicione ao crontab do servidor para executar diariamente às 03:00 da manhã
0 3 * * * /opt/ecommerce-backend/scripts/backup-postgres.sh >> /var/log/pg_backup.log 2>&1
```

---

## 6. Transição Transparente para Nuvem AWS (Fase 2)

O backend possui desacoplamento total de provedor de nuvem via variáveis de ambiente:
- **Armazenamento:** Para migrar do MinIO para Amazon S3 nativo, remova a variável `AWS_ENDPOINT_URL` e preencha as credenciais AWS IAM. O código Java utiliza `software.amazon.awssdk:s3` sem qualquer alteração.
- **Banco de Dados:** Aponte `SPRING_DATASOURCE_URL` para o endpoint do Amazon RDS PostgreSQL Multi-AZ.
- **Cache:** Aponte `SPRING_DATA_REDIS_HOST` para o Amazon ElastiCache Redis.

---

## 7. Próximos Passos (Roadmap de Sprints)

- [x] **Sprint 0:** Fundação de Infraestrutura Portátil (VPS/Docker), Setup de Projeto Java 21, Flyway baseline, RLS PostgreSQL e CI/CD.
- [ ] **Sprint 1:** Multi-Tenancy avançado, RBAC granular e vinculação de usuários.
- [ ] **Sprint 2:** Catálogo de Produtos e Gestão de Estoque em Massa (Livros, ISBN, Variações SKU, Kits e importação em lote).
- [ ] **Sprint 3:** Múltiplos Estoques físicos (Matriz, Balcão, Feiras/Eventos externos) e rotinas de romaneio/conferência cega.
- [ ] **Sprint 4:** PDV Mobile e Venda Balcão / Feira (React PWA com câmera/leitor de código de barras e NFC-e).
- [ ] **Sprint 5:** E-commerce Público, Pagar.me V5 (PIX, Boleto, Cartão) e Vendas a Prazo com trava de inadimplência.
- [ ] **Sprint 6:** Observabilidade completa com OpenTelemetry e testes de carga simulando 100.000 requisições simultâneas.
