package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProcessedWebhookJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProcessedWebhookRepository extends JpaRepository<ProcessedWebhookJpaEntity, UUID> {
    Optional<ProcessedWebhookJpaEntity> findByTenantIdAndGatewayAndEventId(UUID tenantId, String gateway, String eventId);
    boolean existsByTenantIdAndGatewayAndEventId(UUID tenantId, String gateway, String eventId);
}
