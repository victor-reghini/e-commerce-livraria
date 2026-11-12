package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.company.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepositoryPort {

    Company save(Company company);

    Optional<Company> findById(UUID id);

    Optional<Company> findByCodeAndTenantId(String code, UUID tenantId);

    List<Company> findAllByTenantId(UUID tenantId);

    boolean existsByCodeAndTenantId(String code, UUID tenantId);
}

