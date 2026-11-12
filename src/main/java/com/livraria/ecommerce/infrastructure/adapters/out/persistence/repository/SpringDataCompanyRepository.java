package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCompanyRepository extends JpaRepository<CompanyJpaEntity, UUID> {

    Optional<CompanyJpaEntity> findByCodeIgnoreCaseAndTenantId(String code, UUID tenantId);

    List<CompanyJpaEntity> findAllByTenantId(UUID tenantId);

    boolean existsByCodeIgnoreCaseAndTenantId(String code, UUID tenantId);
}

