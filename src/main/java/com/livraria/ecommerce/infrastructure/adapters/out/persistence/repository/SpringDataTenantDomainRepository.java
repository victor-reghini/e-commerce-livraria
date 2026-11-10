package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.TenantDomainJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTenantDomainRepository extends JpaRepository<TenantDomainJpaEntity, UUID> {

    Optional<TenantDomainJpaEntity> findByDomainIgnoreCase(String domain);

    List<TenantDomainJpaEntity> findByTenantId(UUID tenantId);

    boolean existsByDomainIgnoreCase(String domain);
}

