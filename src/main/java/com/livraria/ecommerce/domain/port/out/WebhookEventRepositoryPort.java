package com.livraria.ecommerce.domain.port.out;

import java.util.UUID;

public interface WebhookEventRepositoryPort {
    boolean isAlreadyProcessed(UUID tenantId, String gateway, String eventId);
    void markAsProcessed(UUID tenantId, String gateway, String eventId, String eventType, String payload);
}

