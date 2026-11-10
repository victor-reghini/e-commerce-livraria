package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.model.tenant.TenantDomain;
import com.livraria.ecommerce.domain.port.out.TenantRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.TenantDomainJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.TenantJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.mapper.TenantEntityMapper;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataTenantDomainRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataTenantRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TenantPersistenceAdapter implements TenantRepositoryPort {

    private final SpringDataTenantRepository tenantRepository;
    private final SpringDataTenantDomainRepository tenantDomainRepository;
    private final TenantEntityMapper mapper;

    public TenantPersistenceAdapter(SpringDataTenantRepository tenantRepository,
                                    SpringDataTenantDomainRepository tenantDomainRepository,
                                    TenantEntityMapper mapper) {
        this.tenantRepository = tenantRepository;
        this.tenantDomainRepository = tenantDomainRepository;
        this.mapper = mapper;
    }

    @Override
    public Tenant save(Tenant tenant) {
        TenantJpaEntity entity = mapper.toEntity(tenant);
        TenantJpaEntity saved = tenantRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Tenant> findById(UUID id) {
        return tenantRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return tenantRepository.findBySlug(slug.toLowerCase().trim()).map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findByDomain(String domain) {
        return tenantRepository.findByDomain(domain.toLowerCase().trim()).map(mapper::toDomain);
    }

    @Override
    public List<Tenant> findAll() {
        return tenantRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySlug(String slug) {
        return tenantRepository.existsBySlug(slug.toLowerCase().trim());
    }

    @Override
    public TenantDomain saveDomain(TenantDomain domain) {
        TenantDomainJpaEntity entity = new TenantDomainJpaEntity(
                domain.getId(),
                domain.getTenantId(),
                domain.getDomain().toLowerCase().trim(),
                domain.isPrimary(),
                domain.getCreatedAt()
        );
        TenantDomainJpaEntity saved = tenantDomainRepository.save(entity);
        return new TenantDomain(
                saved.getId(),
                saved.getTenantId(),
                saved.getDomain(),
                saved.isPrimary(),
                saved.getCreatedAt()
        );
    }

    @Override
    public List<TenantDomain> findDomainsByTenantId(UUID tenantId) {
        return tenantDomainRepository.findByTenantId(tenantId).stream()
                .map(entity -> new TenantDomain(
                        entity.getId(),
                        entity.getTenantId(),
                        entity.getDomain(),
                        entity.isPrimary(),
                        entity.getCreatedAt()
                ))
                .toList();
    }
}

