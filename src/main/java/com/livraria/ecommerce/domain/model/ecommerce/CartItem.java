package com.livraria.ecommerce.domain.model.ecommerce;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.util.UUID;

public class CartItem {

    private UUID id;
    private UUID skuId;
    private String skuCode;
    private String productTitle;
    private int quantity;
    private BigDecimal unitPrice;

    public CartItem(UUID id, UUID skuId, String skuCode, String productTitle, int quantity, BigDecimal unitPrice) {
        if (quantity <= 0) {
            throw new BusinessException("A quantidade do item no carrinho deve ser maior que zero.");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O preço unitário do item no carrinho não pode ser negativo.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.skuId = skuId;
        this.skuCode = skuCode;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new BusinessException("A quantidade do item no carrinho deve ser maior que zero.");
        }
        this.quantity = newQuantity;
    }

    public void addQuantity(int additionalQuantity) {
        if (additionalQuantity <= 0) {
            throw new BusinessException("A quantidade a adicionar deve ser maior que zero.");
        }
        this.quantity += additionalQuantity;
    }

    public UUID getId() { return id; }
    public UUID getSkuId() { return skuId; }
    public String getSkuCode() { return skuCode; }
    public String getProductTitle() { return productTitle; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
