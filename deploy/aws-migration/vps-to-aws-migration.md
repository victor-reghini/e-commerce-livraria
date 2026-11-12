# Guia de Migração de Infraestrutura: VPS (Docker) para AWS Cloud

Este documento estabelece o plano arquitetural, runbook operacional e estratégias de contingência para a migração da Plataforma de E-commerce, PDV e Gestão Multi-Tenant de uma infraestrutura VPS autogerenciada (Docker Compose / Swarm) para serviços gerenciados na nuvem da **Amazon Web Services (AWS)** com alta disponibilidade (Multi-AZ), tolerância a falhas e conformidade com LGPD/PCI-DSS.

---

## 1. Topologia Comparativa de Infraestrutura

| Camada | Fase 1: VPS Local / Docker Compose | Fase 2: Nuvem AWS (Produção Enterprise) | Benefício da Transição |
| :--- | :--- | :--- | :--- |
| **Borda / DNS / CDN** | Traefik v3 + Let's Encrypt | **AWS Route 53 + CloudFront CDN + AWS WAF** | Mitigação DDoS L7, latência global < 30ms para mídia |
| **Load Balancer** | Traefik Host Ports | **Application Load Balancer (ALB)** | Descarregamento SSL/TLS via ACM, Auto Scaling Dinâmico |
| **Compute / Backend** | Docker Compose / Swarm VM | **AWS ECS Fargate (Serverless Containers)** | Elasticidade automática sem gerenciar instâncias EC2 |
| **Banco de Dados** | PostgreSQL 16 (Volume local) | **Amazon RDS PostgreSQL 16 Multi-AZ** | Failover automático < 60s, backups contínuos pontuais |
| **Cache & Sessões** | Redis 7 Standalone | **Amazon ElastiCache Redis Cluster (Multi-AZ)** | Replicação em memória, sem perda de sessão de carrinho |
| **Mensageria / Eventos** | RabbitMQ 3 Standalone | **Amazon MQ (RabbitMQ gerenciado)** ou **Amazon SQS** | Alta durabilidade, sem manutenção de brokers |
| **Storage de Arquivos** | MinIO S3-Compatible Local | **Amazon S3 Standard + S3 Glacier Flexible** | 99.999999999% (11 9s) de durabilidade de mídia e NF-e |
| **Observabilidade** | Prometheus + Logback Local | **Amazon Managed Prometheus + Grafana + CloudWatch** | Retenção de longo prazo e alertas 24x7 via PagerDuty |
| **Segurança & Segredos**| Arquivos `.env` locais | **AWS Secrets Manager + IAM Roles** | Rotação automática de credenciais sem deploy |

---

## 2. Mapeamento de Variáveis de Ambiente (Twelve-Factor App)

A aplicação foi desenvolvida segundo os princípios do *Twelve-Factor App*, permitindo a transição para a AWS sem alteração de uma única linha de código fonte:

```env
# Banco de Dados RDS PostgreSQL Multi-AZ
SPRING_DATASOURCE_URL=jdbc:postgresql://ecommerce-db.c9xxxx.us-east-1.rds.amazonaws.com:5432/ecommerce_db
SPRING_DATASOURCE_USERNAME=db_master_admin
SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD_FROM_SECRETS_MANAGER}
HIKARI_MAX_POOL_SIZE=30
HIKARI_MIN_IDLE=10

# Cache ElastiCache Redis
SPRING_DATA_REDIS_HOST=master.ecommerce-redis.xxxx.use1.cache.amazonaws.com
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=${REDIS_AUTH_TOKEN}

# Mensageria Amazon MQ (RabbitMQ)
SPRING_RABBITMQ_HOST=b-xxxx.mq.us-east-1.amazonaws.com
SPRING_RABBITMQ_PORT=5671
SPRING_RABBITMQ_USERNAME=app_broker_user
SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASSWORD}

# Armazenamento S3 (Produção)
AWS_REGION=us-east-1
AWS_S3_PATH_STYLE_ACCESS_ENABLED=false
AWS_S3_BUCKET_MEDIA=ecommerce-media-production-us-east-1
AWS_S3_BUCKET_EBOOKS=ecommerce-ebooks-secure-us-east-1
# Credenciais obtidas automaticamente via IAM Task Execution Role no ECS!
```

---

## 3. Runbook Operacional de Migração (Zero Downtime)

### Passo 1: Migração de Objetos de Mídia (MinIO -> S3)
A sincronização preliminar de imagens de produtos, capas de livros e arquivos digitais é realizada via AWS CLI:
```bash
aws s3 sync s3://ecommerce-media s3://ecommerce-media-production-us-east-1 --endpoint-url http://vps-minio.livraria.com:9000
```

### Passo 2: Migração Contínua do Banco de Dados
1. Configuração do **AWS Database Migration Service (DMS)** ou replicação lógica nativa do PostgreSQL:
   ```sql
   -- No PostgreSQL de origem (VPS)
   ALTER SYSTEM SET wal_level = 'logical';
   ALTER SYSTEM SET max_replication_slots = 5;
   ALTER SYSTEM SET max_wal_senders = 5;
   ```
2. Inicialização da replicação Full Load + CDC (Change Data Capture) para sincronizar dados em tempo real sem interrupção das vendas.

### Passo 3: Deploy das Tasks ECS Fargate
Deploy das novas instâncias no cluster ECS Fargate atrás do ALB. As tarefas de health check apontam para `/actuator/health`.

### Passo 4: Janela de Cutover (Virada de DNS)
1. Colocar o front-end em modo de manutenção leve por 2 minutos para drenagem de conexões.
2. Promover o RDS PostgreSQL para primário de escrita.
3. Atualizar registros DNS no **AWS Route 53** apontando para a distribuição do **CloudFront**.
4. Validar transações de checkout e emissão de NF-e na SEFAZ.
5. Concluir a virada de tráfego.

