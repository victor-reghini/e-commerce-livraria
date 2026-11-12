package com.livraria.ecommerce.domain.model.ecommerce;

import java.math.BigDecimal;
import java.util.UUID;

public class CorporateBudgetItem {

    private UUID id;
    private UUID skuId;
    private String skuCode;
    private String productTitle;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal total;

    public CorporateBudgetItem(UUID id, UUID skuId, String skuCode, String productTitle, int quantity,
                               BigDecimal unitPrice, BigDecimal discount, BigDecimal total) {
        this.id = id != null ? id : UUID.randomUUID();
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.total = total != null ? total : unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(this.discount);
    }

    public UUID getId() { return id; }
    public UUID getSkuId() { return skuId; }
    public String getSkuCode() { return skuCode; }
    public String getProductTitle() { return productTitle; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
}
