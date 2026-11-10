package com.livraria.ecommerce.domain.port.out;

import java.io.InputStream;
import java.time.Duration;

public interface StoragePort {

    String upload(String bucket, String key, InputStream inputStream, long contentLength, String contentType);

    byte[] download(String bucket, String key);

    String generatePresignedDownloadUrl(String bucket, String key, Duration duration);

    String generatePresignedUploadUrl(String bucket, String key, Duration duration);

    void delete(String bucket, String key);

    boolean exists(String bucket, String key);
}

