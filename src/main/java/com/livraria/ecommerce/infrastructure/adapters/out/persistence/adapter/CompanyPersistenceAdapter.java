package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.port.out.CompanyRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.mapper.CompanyEntityMapper;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CompanyPersistenceAdapter implements CompanyRepositoryPort {

    private final SpringDataCompanyRepository repository;
    private final CompanyEntityMapper mapper;

    public CompanyPersistenceAdapter(SpringDataCompanyRepository repository, CompanyEntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Company save(Company company) {
        CompanyJpaEntity entity = mapper.toEntity(company);
        CompanyJpaEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Company> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Company> findByCodeAndTenantId(String code, UUID tenantId) {
        UUID effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantId().orElse(null);
        return repository.findByCodeIgnoreCaseAndTenantId(code.toUpperCase().trim(), effectiveTenantId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Company> findAllByTenantId(UUID tenantId) {
        UUID effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantId().orElse(null);
        return repository.findAllByTenantId(effectiveTenantId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByCodeAndTenantId(String code, UUID tenantId) {
        UUID effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantId().orElse(null);
        return repository.existsByCodeIgnoreCaseAndTenantId(code.toUpperCase().trim(), effectiveTenantId);
    }
}

