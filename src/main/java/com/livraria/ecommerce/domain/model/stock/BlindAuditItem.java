package com.livraria.ecommerce.domain.model.stock;

import java.time.Instant;
import java.util.UUID;

public class BlindAuditItem {

    private UUID id;
    private UUID tenantId;
    private UUID auditId;
    private UUID skuId;
    private String skuCode;
    private String title;
    private int quantityScanned;
    private int quantityExpected;
    private int divergence;
    private Instant lastScannedAt;

    public BlindAuditItem(UUID id, UUID tenantId, UUID auditId, UUID skuId, String skuCode, String title,
                          int quantityScanned, int quantityExpected, int divergence, Instant lastScannedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.auditId = auditId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.title = title;
        this.quantityScanned = Math.max(0, quantityScanned);
        this.quantityExpected = Math.max(0, quantityExpected);
        this.divergence = this.quantityScanned - this.quantityExpected;
        this.lastScannedAt = lastScannedAt != null ? lastScannedAt : Instant.now();
    }

    public void incrementScanned(int count) {
        this.quantityScanned += Math.max(1, count);
        this.divergence = this.quantityScanned - this.quantityExpected;
        this.lastScannedAt = Instant.now();
    }

    public void setQuantityExpected(int expected) {
        this.quantityExpected = Math.max(0, expected);
        this.divergence = this.quantityScanned - this.quantityExpected;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getAuditId() { return auditId; }
    public UUID getSkuId() { return skuId; }
    public String getSkuCode() { return skuCode; }
    public String getTitle() { return title; }
    public int getQuantityScanned() { return quantityScanned; }
    public int getQuantityExpected() { return quantityExpected; }
    public int getDivergence() { return divergence; }
    public Instant getLastScannedAt() { return lastScannedAt; }
}
