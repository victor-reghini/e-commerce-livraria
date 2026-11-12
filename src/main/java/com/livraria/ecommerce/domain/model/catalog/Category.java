package com.livraria.ecommerce.domain.model.catalog;

import java.time.Instant;
import java.util.UUID;

public class Category {

    private UUID id;
    private UUID tenantId;
    private String name;
    private String slug;
    private UUID parentId;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public Category(UUID id, UUID tenantId, String name, String slug, UUID parentId, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.slug = slug;
        this.parentId = parentId;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Category create(UUID tenantId, String name, String slug, UUID parentId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório.");
        }
        if (slug == null || slug.isBlank()) {
            slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        }
        return new Category(UUID.randomUUID(), tenantId, name, slug, parentId, true, Instant.now(), Instant.now());
    }

    public void update(String name, String slug, UUID parentId, boolean active) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório.");
        }
        this.name = name;
        if (slug != null && !slug.isBlank()) {
            this.slug = slug;
        }
        this.parentId = parentId;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public UUID getParentId() { return parentId; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

