package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.OrderItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataOrderItemRepository extends JpaRepository<OrderItemJpaEntity, UUID> {
    List<OrderItemJpaEntity> findAllByTenantIdAndOrderId(UUID tenantId, UUID orderId);
}

