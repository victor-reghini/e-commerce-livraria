package com.livraria.ecommerce.application.dto;

import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.model.tenant.TenantStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record TenantDto(
        UUID id,
        String slug,
        String name,
        String tradeName,
        String cnpj,
        String email,
        String phone,
        TenantStatus status,
        Map<String, Object> settings,
        Instant createdAt,
        Instant updatedAt
) {
    public static TenantDto fromDomain(Tenant tenant) {
        return new TenantDto(
                tenant.getId(),
                tenant.getSlug(),
                tenant.getName(),
                tenant.getTradeName(),
                tenant.getCnpj(),
                tenant.getEmail(),
                tenant.getPhone(),
                tenant.getStatus(),
                tenant.getSettings(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}

