package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.OrderPaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataOrderPaymentRepository extends JpaRepository<OrderPaymentJpaEntity, UUID> {
    List<OrderPaymentJpaEntity> findAllByTenantIdAndOrderId(UUID tenantId, UUID orderId);
    Optional<OrderPaymentJpaEntity> findByTenantIdAndGatewayTransactionId(UUID tenantId, String gatewayTransactionId);
}

