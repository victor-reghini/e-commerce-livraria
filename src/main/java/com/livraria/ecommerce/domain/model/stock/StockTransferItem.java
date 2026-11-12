package com.livraria.ecommerce.domain.model.stock;

import java.math.BigDecimal;
import java.util.UUID;

public class StockTransferItem {

    private UUID id;
    private UUID tenantId;
    private UUID transferId;
    private UUID skuId;
    private String skuCode;
    private String title;
    private int quantityRequested;
    private int quantityDispatched;
    private int quantityReceived;
    private BigDecimal unitCost;
    private BigDecimal unitPrice;

    public StockTransferItem(UUID id, UUID tenantId, UUID transferId, UUID skuId, String skuCode, String title,
                             int quantityRequested, int quantityDispatched, int quantityReceived,
                             BigDecimal unitCost, BigDecimal unitPrice) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.transferId = transferId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.title = title;
        this.quantityRequested = Math.max(1, quantityRequested);
        this.quantityDispatched = Math.max(0, quantityDispatched);
        this.quantityReceived = Math.max(0, quantityReceived);
        this.unitCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
    }

    public boolean isFullyReceived() {
        return quantityReceived == quantityDispatched;
    }

    public int getDivergence() {
        return quantityReceived - quantityDispatched;
    }

    public void setQuantityDispatched(int quantityDispatched) {
        this.quantityDispatched = Math.max(0, quantityDispatched);
    }

    public void setQuantityReceived(int quantityReceived) {
        this.quantityReceived = Math.max(0, quantityReceived);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getTransferId() { return transferId; }
    public UUID getSkuId() { return skuId; }
    public String getSkuCode() { return skuCode; }
    public String getTitle() { return title; }
    public int getQuantityRequested() { return quantityRequested; }
    public int getQuantityDispatched() { return quantityDispatched; }
    public int getQuantityReceived() { return quantityReceived; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
