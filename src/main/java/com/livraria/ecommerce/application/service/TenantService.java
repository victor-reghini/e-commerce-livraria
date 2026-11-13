package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.TenantNotFoundException;
import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.model.tenant.TenantDomain;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import com.livraria.ecommerce.domain.port.in.TenantUseCase;
import com.livraria.ecommerce.domain.port.out.TenantRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantService implements TenantUseCase {

    private final TenantRepositoryPort tenantRepository;
    private final AuditUseCase auditUseCase;

    @Autowired
    public TenantService(TenantRepositoryPort tenantRepository,
                         @Autowired(required = false) AuditUseCase auditUseCase) {
        this.tenantRepository = tenantRepository;
        this.auditUseCase = auditUseCase;
    }

    public TenantService(TenantRepositoryPort tenantRepository) {
        this(tenantRepository, null);
    }

    @Override
    @Transactional
    public Tenant createTenant(CreateTenantCommand command) {
        if (tenantRepository.existsBySlug(command.slug())) {
            throw new BusinessException("Já existe um tenant cadastrado com o slug: " + command.slug());
        }

        Tenant tenant = Tenant.create(
                command.slug(),
                command.name(),
                command.tradeName(),
                command.cnpj(),
                command.email(),
                command.phone(),
                command.settings()
        );

        Tenant savedTenant = tenantRepository.save(tenant);

        if (command.primaryDomain() != null && !command.primaryDomain().isBlank()) {
            TenantDomain primaryDomain = TenantDomain.create(savedTenant.getId(), command.primaryDomain(), true);
            tenantRepository.saveDomain(primaryDomain);
        }

        if (auditUseCase != null) {
            auditUseCase.recordEvent("TENANT_CREATED", "TENANT", savedTenant.getId().toString(), Map.of(
                    "name", savedTenant.getName(),
                    "slug", savedTenant.getSlug(),
                    "cnpj", savedTenant.getCnpj()
            ));
        }

        return savedTenant;
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant getTenantById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new TenantNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant getTenantBySlug(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new TenantNotFoundException("slug=" + slug));
    }

    @Override
    @Transactional(readOnly = true)
    public Tenant getTenantByDomain(String domain) {
        return tenantRepository.findByDomain(domain)
                .orElseThrow(() -> new TenantNotFoundException("domain=" + domain));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tenant> listAllTenants() {
        return tenantRepository.findAll();
    }

    @Override
    @Transactional
    public void linkDomain(UUID tenantId, String domain, boolean primary) {
        Tenant tenant = getTenantById(tenantId);
        TenantDomain tenantDomain = TenantDomain.create(tenant.getId(), domain, primary);
        tenantRepository.saveDomain(tenantDomain);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("TENANT_DOMAIN_LINKED", "TENANT", tenantId.toString(), Map.of(
                    "domain", domain,
                    "primary", primary
            ));
        }
    }
}

