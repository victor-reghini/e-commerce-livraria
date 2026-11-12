package com.livraria.ecommerce.domain.model.stock;

import java.time.Instant;
import java.util.UUID;

public class StockMovement {

    private UUID id;
    private UUID tenantId;
    private UUID companyId;
    private UUID skuId;
    private MovementType movementType;
    private int quantityChange;
    private int balanceAfter;
    private UUID referenceId;
    private String reason;
    private UUID createdBy;
    private Instant createdAt;

    public StockMovement(UUID id, UUID tenantId, UUID companyId, UUID skuId, MovementType movementType,
                         int quantityChange, int balanceAfter, UUID referenceId, String reason,
                         UUID createdBy, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.skuId = skuId;
        this.movementType = movementType;
        this.quantityChange = quantityChange;
        this.balanceAfter = balanceAfter;
        this.referenceId = referenceId;
        this.reason = reason;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCompanyId() { return companyId; }
    public UUID getSkuId() { return skuId; }
    public MovementType getMovementType() { return movementType; }
    public int getQuantityChange() { return quantityChange; }
    public int getBalanceAfter() { return balanceAfter; }
    public UUID getReferenceId() { return referenceId; }
    public String getReason() { return reason; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
