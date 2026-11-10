#!/bin/sh
# ==============================================================================
# docker/minio/init-buckets.sh
# Inicialização automática de Buckets e Políticas de Acesso no MinIO
# ==============================================================================

set -e

echo "Aguardando inicialização do serviço MinIO..."
until /usr/bin/mc alias set myminio "http://${MINIO_HOST:-minio}:${MINIO_PORT:-9000}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}"; do
  echo "MinIO ainda não está pronto. Tentando novamente em 2 segundos..."
  sleep 2
done

echo "MinIO disponível. Configurando buckets padrão..."

# 1. Bucket público para mídias (capas de livros, fotos de produtos, banners)
/usr/bin/mc mb --ignore-existing myminio/"${AWS_S3_BUCKET_MEDIA:-ecommerce-media}"
/usr/bin/mc anonymous set download myminio/"${AWS_S3_BUCKET_MEDIA:-ecommerce-media}"

# 2. Bucket privado para arquivos protegidos (E-books, comprovantes, relatórios fiscais)
/usr/bin/mc mb --ignore-existing myminio/"${AWS_S3_BUCKET_EBOOKS:-ecommerce-ebooks}"
/usr/bin/mc anonymous set none myminio/"${AWS_S3_BUCKET_EBOOKS:-ecommerce-ebooks}"

echo "Buckets configurados com sucesso:"
/usr/bin/mc ls myminio

exit 0

