package com.livraria.ecommerce.domain.model.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public class BundleItem {

    private UUID id;
    private UUID tenantId;
    private UUID bundleSkuId;
    private UUID itemSkuId;
    private String itemSkuCode;
    private String itemTitle;
    private BigDecimal itemPrice;
    private String itemNcm;
    private String itemCfop;
    private int quantity;
    private BundleType bundleType;
    private BigDecimal discountPercentage;

    public BundleItem(UUID id, UUID tenantId, UUID bundleSkuId, UUID itemSkuId, String itemSkuCode,
                      String itemTitle, BigDecimal itemPrice, String itemNcm, String itemCfop,
                      int quantity, BundleType bundleType, BigDecimal discountPercentage) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.bundleSkuId = bundleSkuId;
        this.itemSkuId = itemSkuId;
        this.itemSkuCode = itemSkuCode;
        this.itemTitle = itemTitle;
        this.itemPrice = itemPrice != null ? itemPrice : BigDecimal.ZERO;
        this.itemNcm = itemNcm;
        this.itemCfop = itemCfop;
        this.quantity = Math.max(1, quantity);
        this.bundleType = bundleType != null ? bundleType : BundleType.DYNAMIC_VIRTUAL;
        this.discountPercentage = discountPercentage != null ? discountPercentage : BigDecimal.ZERO;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getBundleSkuId() { return bundleSkuId; }
    public UUID getItemSkuId() { return itemSkuId; }
    public String getItemSkuCode() { return itemSkuCode; }
    public String getItemTitle() { return itemTitle; }
    public BigDecimal getItemPrice() { return itemPrice; }
    public String getItemNcm() { return itemNcm; }
    public String getItemCfop() { return itemCfop; }
    public int getQuantity() { return quantity; }
    public BundleType getBundleType() { return bundleType; }
    public BigDecimal getDiscountPercentage() { return discountPercentage; }
}

