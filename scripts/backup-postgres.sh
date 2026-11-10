#!/usr/bin/env bash
# ==============================================================================
# scripts/backup-postgres.sh
# Backup automatizado do PostgreSQL com compressão e rotação de snapshots (7 dias)
# Uso recomendado via crontab:
# 0 3 * * * /caminho/do/projeto/scripts/backup-postgres.sh >> /var/log/pg_backup.log 2>&1
# ==============================================================================

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/var/backups/ecommerce-postgres}"
RETENTION_DAYS="${RETENTION_DAYS:-7}"
TIMESTAMP="$(date +'%Y%m%d_%H%M%S')"
CONTAINER_NAME="${PG_CONTAINER_NAME:-ecommerce-postgres}"
DB_NAME="${DATABASE_NAME:-ecommerce_db}"
DB_USER="${DATABASE_USER:-ecommerce_user}"

mkdir -p "${BACKUP_DIR}"

BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Iniciando backup do banco de dados: ${DB_NAME} a partir do container: ${CONTAINER_NAME}..."

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "[ERRO] Container ${CONTAINER_NAME} não está em execução!" >&2
    exit 1
fi

docker exec -t "${CONTAINER_NAME}" pg_dump -U "${DB_USER}" -d "${DB_NAME}" --clean --if-exists | gzip > "${BACKUP_FILE}"

FILESIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Backup concluído com sucesso: ${BACKUP_FILE} (Tamanho: ${FILESIZE})"

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Removendo snapshots com mais de ${RETENTION_DAYS} dias..."
find "${BACKUP_DIR}" -type f -name "${DB_NAME}_*.sql.gz" -mtime +"${RETENTION_DAYS}" -exec rm -vf {} \;

echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] Rotina de backup finalizada."

