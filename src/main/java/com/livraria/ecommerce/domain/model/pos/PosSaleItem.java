package com.livraria.ecommerce.domain.model.pos;

import java.math.BigDecimal;
import java.util.UUID;

public class PosSaleItem {

    private UUID id;
    private UUID tenantId;
    private UUID saleId;
    private UUID skuId;
    private String skuCode;
    private String title;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal subtotal;

    public PosSaleItem(UUID id, UUID tenantId, UUID saleId, UUID skuId, String skuCode, String title,
                       int quantity, BigDecimal unitPrice, BigDecimal discount, BigDecimal subtotal) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.saleId = saleId;
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.title = title;
        this.quantity = Math.max(1, quantity);
        this.unitPrice = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.subtotal = subtotal != null ? subtotal : this.unitPrice.multiply(BigDecimal.valueOf(this.quantity)).subtract(this.discount);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSaleId() { return saleId; }
    public UUID getSkuId() { return skuId; }
    public String getSkuCode() { return skuCode; }
    public String getTitle() { return title; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getSubtotal() { return subtotal; }
}
