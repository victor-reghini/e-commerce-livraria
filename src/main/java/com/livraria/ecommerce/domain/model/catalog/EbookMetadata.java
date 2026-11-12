package com.livraria.ecommerce.domain.model.catalog;

import java.util.UUID;

public class EbookMetadata {

    private UUID id;
    private UUID tenantId;
    private UUID skuId;
    private String fileS3Key;
    private String fileFormat;
    private long fileSizeBytes;
    private boolean drmEnabled;

    public EbookMetadata(UUID id, UUID tenantId, UUID skuId, String fileS3Key, String fileFormat, long fileSizeBytes, boolean drmEnabled) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.skuId = skuId;
        this.fileS3Key = fileS3Key;
        this.fileFormat = fileFormat != null ? fileFormat.toUpperCase() : "EPUB";
        this.fileSizeBytes = fileSizeBytes;
        this.drmEnabled = drmEnabled;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSkuId() { return skuId; }
    public String getFileS3Key() { return fileS3Key; }
    public String getFileFormat() { return fileFormat; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public boolean isDrmEnabled() { return drmEnabled; }
}

