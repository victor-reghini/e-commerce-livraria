package com.livraria.ecommerce.domain.model.tenant;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TenantDomain {

    private final UUID id;
    private final UUID tenantId;
    private final String domain;
    private final boolean primary;
    private final Instant createdAt;

    public TenantDomain(UUID id, UUID tenantId, String domain, boolean primary, Instant createdAt) {
        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID é obrigatório");
        this.domain = Objects.requireNonNull(domain, "Domínio é obrigatório").toLowerCase().trim();
        this.primary = primary;
        this.createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
    }

    public static TenantDomain create(UUID tenantId, String domain, boolean primary) {
        return new TenantDomain(UUID.randomUUID(), tenantId, domain, primary, Instant.now());
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getDomain() {
        return domain;
    }

    public boolean isPrimary() {
        return primary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

