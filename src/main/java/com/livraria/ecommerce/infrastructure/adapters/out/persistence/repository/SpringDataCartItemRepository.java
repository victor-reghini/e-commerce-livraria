package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CartItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCartItemRepository extends JpaRepository<CartItemJpaEntity, UUID> {
    Optional<CartItemJpaEntity> findByTenantIdAndCartIdAndSkuId(UUID tenantId, UUID cartId, UUID skuId);
    void deleteAllByTenantIdAndCartId(UUID tenantId, UUID cartId);
}

