package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.domain.model.tenant.Tenant;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TenantUseCase {

    Tenant createTenant(CreateTenantCommand command);

    Tenant getTenantById(UUID id);

    Tenant getTenantBySlug(String slug);

    Tenant getTenantByDomain(String domain);

    List<Tenant> listAllTenants();

    void linkDomain(UUID tenantId, String domain, boolean primary);

    record CreateTenantCommand(
            String slug,
            String name,
            String tradeName,
            String cnpj,
            String email,
            String phone,
            Map<String, Object> settings,
            String primaryDomain
    ) {}
}

