package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.ecommerce.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepositoryPort {
    Optional<Cart> findById(UUID id);
    Optional<Cart> findByTenantIdAndSessionId(UUID tenantId, String sessionId);
    Optional<Cart> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    Cart save(Cart cart);
    void delete(UUID id);
}

