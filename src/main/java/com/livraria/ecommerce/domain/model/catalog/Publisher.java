package com.livraria.ecommerce.domain.model.catalog;

import java.time.Instant;
import java.util.UUID;

public class Publisher {

    private UUID id;
    private UUID tenantId;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;

    public Publisher(UUID id, UUID tenantId, String name, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Publisher create(UUID tenantId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da editora é obrigatório.");
        }
        return new Publisher(UUID.randomUUID(), tenantId, name.trim(), Instant.now(), Instant.now());
    }

    public void update(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da editora é obrigatório.");
        }
        this.name = name.trim();
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

