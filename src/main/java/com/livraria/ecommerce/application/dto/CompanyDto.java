package com.livraria.ecommerce.application.dto;

import com.livraria.ecommerce.domain.model.company.Address;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.model.company.TaxRegime;

import java.time.Instant;
import java.util.UUID;

public record CompanyDto(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String tradeName,
        String cnpj,
        String stateRegistration,
        String municipalRegistration,
        TaxRegime taxRegime,
        boolean headquarters,
        boolean active,
        Address address,
        Instant createdAt,
        Instant updatedAt
) {
    public static CompanyDto fromDomain(Company company) {
        return new CompanyDto(
                company.getId(),
                company.getTenantId(),
                company.getCode(),
                company.getName(),
                company.getTradeName(),
                company.getCnpj(),
                company.getStateRegistration(),
                company.getMunicipalRegistration(),
                company.getTaxRegime(),
                company.isHeadquarters(),
                company.isActive(),
                company.getAddress(),
                company.getCreatedAt(),
                company.getUpdatedAt()
        );
    }
}

