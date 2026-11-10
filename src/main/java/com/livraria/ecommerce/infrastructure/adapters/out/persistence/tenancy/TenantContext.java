package com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy;

import java.util.Optional;
import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static void setTenantId(String tenantIdStr) {
        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            try {
                CURRENT_TENANT.set(UUID.fromString(tenantIdStr.trim()));
            } catch (IllegalArgumentException e) {
                CURRENT_TENANT.remove();
            }
        } else {
            CURRENT_TENANT.remove();
        }
    }

    public static Optional<UUID> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static String getTenantIdAsString() {
        UUID id = CURRENT_TENANT.get();
        return id != null ? id.toString() : null;
    }

    public static UUID getRequiredTenantId() {
        return getTenantId()
                .orElseThrow(() -> new IllegalStateException("Nenhum tenant associado à requisição atual."));
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

