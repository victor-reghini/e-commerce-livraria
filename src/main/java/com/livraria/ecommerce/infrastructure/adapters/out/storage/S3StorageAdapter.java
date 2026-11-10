package com.livraria.ecommerce.infrastructure.adapters.out.storage;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.port.out.StoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;

@Component
public class S3StorageAdapter implements StoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3StorageAdapter.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3StorageAdapter(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public String upload(String bucket, String key, InputStream inputStream, long contentLength, String contentType) {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            log.info("Arquivo armazenado com sucesso no S3/MinIO: bucket={}, key={}", bucket, key);
            return key;
        } catch (Exception e) {
            log.error("Erro ao fazer upload para S3/MinIO: bucket={}, key={}", bucket, key, e);
            throw new BusinessException("Falha no upload do arquivo para o storage: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String bucket, String key) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
            return responseBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new BusinessException("Arquivo não encontrado no storage: " + key, e);
        } catch (Exception e) {
            log.error("Erro ao baixar arquivo do S3/MinIO: bucket={}, key={}", bucket, key, e);
            throw new BusinessException("Falha no download do arquivo do storage: " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String bucket, String key, Duration duration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Erro ao gerar URL pré-assinada de download: bucket={}, key={}", bucket, key, e);
            throw new BusinessException("Falha ao gerar URL assinada de download: " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePresignedUploadUrl(String bucket, String key, Duration duration) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .putObjectRequest(putObjectRequest)
                    .build();

            return s3Presigner.presignPutObject(presignRequest).url().toString();
        } catch (Exception e) {
            log.error("Erro ao gerar URL pré-assinada de upload: bucket={}, key={}", bucket, key, e);
            throw new BusinessException("Falha ao gerar URL assinada de upload: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
            log.info("Arquivo removido do storage: bucket={}, key={}", bucket, key);
        } catch (Exception e) {
            log.error("Erro ao remover arquivo do storage: bucket={}, key={}", bucket, key, e);
            throw new BusinessException("Falha ao excluir arquivo do storage: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String bucket, String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

