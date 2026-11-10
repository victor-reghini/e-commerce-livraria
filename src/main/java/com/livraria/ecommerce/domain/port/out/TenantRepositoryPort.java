package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.model.tenant.TenantDomain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepositoryPort {

    Tenant save(Tenant tenant);

    Optional<Tenant> findById(UUID id);

    Optional<Tenant> findBySlug(String slug);

    Optional<Tenant> findByDomain(String domain);

    List<Tenant> findAll();

    boolean existsBySlug(String slug);

    TenantDomain saveDomain(TenantDomain tenantDomain);

    List<TenantDomain> findDomainsByTenantId(UUID tenantId);
}

