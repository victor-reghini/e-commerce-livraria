package com.livraria.ecommerce.domain.model.catalog;

import java.time.Instant;
import java.util.UUID;

public class Author {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String biography;
    private Instant createdAt;
    private Instant updatedAt;

    public Author(UUID id, UUID tenantId, String name, String biography, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.biography = biography;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Author create(UUID tenantId, String name, String biography) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do autor é obrigatório.");
        }
        return new Author(UUID.randomUUID(), tenantId, name.trim(), biography, Instant.now(), Instant.now());
    }

    public void update(String name, String biography) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome do autor é obrigatório.");
        }
        this.name = name.trim();
        this.biography = biography;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getBiography() { return biography; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

