package com.livraria.ecommerce.infrastructure.adapters.out.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.TenantJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
public class TenantEntityMapper {

    private final ObjectMapper objectMapper;

    public TenantEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Tenant toDomain(TenantJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Map<String, Object> settings = Collections.emptyMap();
        if (entity.getSettingsJson() != null && !entity.getSettingsJson().isBlank()) {
            try {
                settings = objectMapper.readValue(entity.getSettingsJson(), new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                // fallback to empty settings
            }
        }

        return new Tenant(
                entity.getId(),
                entity.getSlug(),
                entity.getName(),
                entity.getTradeName(),
                entity.getCnpj(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getStatus(),
                settings,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public TenantJpaEntity toEntity(Tenant domain) {
        if (domain == null) {
            return null;
        }

        String settingsJson = "{}";
        if (domain.getSettings() != null && !domain.getSettings().isEmpty()) {
            try {
                settingsJson = objectMapper.writeValueAsString(domain.getSettings());
            } catch (JsonProcessingException e) {
                // fallback to empty json
            }
        }

        return new TenantJpaEntity(
                domain.getId(),
                domain.getSlug(),
                domain.getName(),
                domain.getTradeName(),
                domain.getCnpj(),
                domain.getEmail(),
                domain.getPhone(),
                domain.getStatus(),
                settingsJson,
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}

