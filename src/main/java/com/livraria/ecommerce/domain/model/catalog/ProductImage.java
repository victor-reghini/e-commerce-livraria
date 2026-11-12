package com.livraria.ecommerce.domain.model.catalog;

import java.util.UUID;

public class ProductImage {

    private UUID id;
    private UUID tenantId;
    private UUID productId;
    private UUID skuId;
    private String imageUrl;
    private String s3Key;
    private boolean primary;
    private int displayOrder;

    public ProductImage(UUID id, UUID tenantId, UUID productId, UUID skuId, String imageUrl, String s3Key, boolean primary, int displayOrder) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.productId = productId;
        this.skuId = skuId;
        this.imageUrl = imageUrl;
        this.s3Key = s3Key;
        this.primary = primary;
        this.displayOrder = displayOrder;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProductId() { return productId; }
    public UUID getSkuId() { return skuId; }
    public String getImageUrl() { return imageUrl; }
    public String getS3Key() { return s3Key; }
    public boolean isPrimary() { return primary; }
    public int getDisplayOrder() { return displayOrder; }
}

