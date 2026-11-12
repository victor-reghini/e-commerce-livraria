package com.livraria.ecommerce.domain.model.ecommerce;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    private UUID id;
    private UUID skuId;
    private String skuCode;
    private String productTitle;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discount;
    private BigDecimal total;
    private String ncm;
    private String cfop;

    public OrderItem(UUID id, UUID skuId, String skuCode, String productTitle, int quantity,
                     BigDecimal unitPrice, BigDecimal discount, BigDecimal total, String ncm, String cfop) {
        this.id = id != null ? id : UUID.randomUUID();
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.total = total != null ? total : unitPrice.multiply(BigDecimal.valueOf(quantity)).subtract(this.discount);
        this.ncm = ncm;
        this.cfop = cfop;
    }

    public UUID getId() { return id; }
    public UUID getSkuId() { return skuId; }
    public String getSkuCode() { return skuCode; }
    public String getProductTitle() { return productTitle; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public String getNcm() { return ncm; }
    public String getCfop() { return cfop; }
}
