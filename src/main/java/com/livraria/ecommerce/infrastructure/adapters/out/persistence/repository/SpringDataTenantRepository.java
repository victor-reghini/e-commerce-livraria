package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.TenantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataTenantRepository extends JpaRepository<TenantJpaEntity, UUID> {

    Optional<TenantJpaEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("SELECT t FROM TenantJpaEntity t JOIN TenantDomainJpaEntity d ON t.id = d.tenantId WHERE LOWER(d.domain) = LOWER(:domain)")
    Optional<TenantJpaEntity> findByDomain(@Param("domain") String domain);
}

