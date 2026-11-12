package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.port.out.WebhookEventRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProcessedWebhookJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProcessedWebhookRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
public class WebhookEventPersistenceAdapter implements WebhookEventRepositoryPort {

    private final SpringDataProcessedWebhookRepository webhookRepository;

    public WebhookEventPersistenceAdapter(SpringDataProcessedWebhookRepository webhookRepository) {
        this.webhookRepository = webhookRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(UUID tenantId, String gateway, String eventId) {
        return webhookRepository.existsByTenantIdAndGatewayAndEventId(tenantId, gateway, eventId);
    }

    @Override
    @Transactional
    public void markAsProcessed(UUID tenantId, String gateway, String eventId, String eventType, String payload) {
        if (!webhookRepository.existsByTenantIdAndGatewayAndEventId(tenantId, gateway, eventId)) {
            ProcessedWebhookJpaEntity entity = ProcessedWebhookJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .gateway(gateway)
                    .eventId(eventId)
                    .eventType(eventType)
                    .payload(payload)
                    .status("PROCESSED")
                    .processedAt(Instant.now())
                    .build();
            webhookRepository.save(entity);
        }
    }
}
