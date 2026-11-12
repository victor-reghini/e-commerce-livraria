package com.livraria.ecommerce.infrastructure.adapters.out.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.domain.model.company.Address;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CompanyEntityMapper {

    private final ObjectMapper objectMapper;

    public CompanyEntityMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Company toDomain(CompanyJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Address address = null;
        if (entity.getAddressJson() != null && !entity.getAddressJson().isBlank()) {
            try {
                address = objectMapper.readValue(entity.getAddressJson(), Address.class);
            } catch (JsonProcessingException e) {
                // fallback to null
            }
        }

        return new Company(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getName(),
                entity.getTradeName(),
                entity.getCnpj(),
                entity.getStateRegistration(),
                entity.getMunicipalRegistration(),
                entity.getTaxRegime(),
                entity.isHeadquarters(),
                entity.isActive(),
                address,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CompanyJpaEntity toEntity(Company domain) {
        if (domain == null) {
            return null;
        }

        String addressJson = "{}";
        if (domain.getAddress() != null) {
            try {
                addressJson = objectMapper.writeValueAsString(domain.getAddress());
            } catch (JsonProcessingException e) {
                // fallback to empty json
            }
        }

        return new CompanyJpaEntity(
                domain.getId(),
                domain.getTenantId(),
                domain.getCode(),
                domain.getName(),
                domain.getTradeName(),
                domain.getCnpj(),
                domain.getStateRegistration(),
                domain.getMunicipalRegistration(),
                domain.getTaxRegime(),
                domain.isHeadquarters(),
                domain.isActive(),
                addressJson,
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}

