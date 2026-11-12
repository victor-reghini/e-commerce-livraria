package com.livraria.ecommerce.domain.model.ecommerce;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Cart {

    private UUID id;
    private UUID tenantId;
    private String sessionId;
    private UUID customerId;
    private List<CartItem> items;
    private Instant createdAt;
    private Instant updatedAt;

    public Cart(UUID id, UUID tenantId, String sessionId, UUID customerId, List<CartItem> items, Instant createdAt, Instant updatedAt) {
        if (tenantId == null) {
            throw new BusinessException("Tenant ID é obrigatório para o carrinho de compras.");
        }
        if ((sessionId == null || sessionId.isBlank()) && customerId == null) {
            throw new BusinessException("O carrinho deve estar vinculado a uma sessão ou a um cliente autenticado.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.customerId = customerId;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Cart createNew(UUID tenantId, String sessionId, UUID customerId) {
        return new Cart(UUID.randomUUID(), tenantId, sessionId, customerId, new ArrayList<>(), Instant.now(), Instant.now());
    }

    public void addItem(UUID skuId, String skuCode, String productTitle, int quantity, BigDecimal unitPrice) {
        if (skuId == null) {
            throw new BusinessException("SKU ID é obrigatório para adicionar item ao carrinho.");
        }
        Optional<CartItem> existing = items.stream().filter(item -> item.getSkuId().equals(skuId)).findFirst();
        if (existing.isPresent()) {
            existing.get().addQuantity(quantity);
        } else {
            items.add(new CartItem(UUID.randomUUID(), skuId, skuCode, productTitle, quantity, unitPrice));
        }
        this.updatedAt = Instant.now();
    }

    public void updateItemQuantity(UUID skuId, int newQuantity) {
        if (newQuantity <= 0) {
            removeItem(skuId);
            return;
        }
        CartItem item = items.stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Item com SKU informado não encontrado no carrinho."));
        item.updateQuantity(newQuantity);
        this.updatedAt = Instant.now();
    }

    public void removeItem(UUID skuId) {
        items.removeIf(item -> item.getSkuId().equals(skuId));
        this.updatedAt = Instant.now();
    }

    public void clear() {
        items.clear();
        this.updatedAt = Instant.now();
    }

    public void assignCustomer(UUID customerId) {
        this.customerId = customerId;
        this.updatedAt = Instant.now();
    }

    public BigDecimal calculateSubtotal() {
        return items.stream()
                .map(CartItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int calculateTotalItems() {
        return items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getSessionId() { return sessionId; }
    public UUID getCustomerId() { return customerId; }
    public List<CartItem> getItems() { return Collections.unmodifiableList(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

