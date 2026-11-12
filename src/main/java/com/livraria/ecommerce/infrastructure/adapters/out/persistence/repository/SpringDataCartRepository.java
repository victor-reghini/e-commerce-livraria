package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CartJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCartRepository extends JpaRepository<CartJpaEntity, UUID> {
    Optional<CartJpaEntity> findByTenantIdAndSessionId(UUID tenantId, String sessionId);
    Optional<CartJpaEntity> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
