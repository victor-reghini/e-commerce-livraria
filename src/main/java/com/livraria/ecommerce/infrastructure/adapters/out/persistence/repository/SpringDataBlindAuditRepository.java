package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.BlindAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataBlindAuditRepository extends JpaRepository<BlindAuditJpaEntity, UUID> {

    Optional<BlindAuditJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<BlindAuditJpaEntity> findByTenantIdAndCode(UUID tenantId, String code);

    List<BlindAuditJpaEntity> findAllByTenantIdAndCompanyIdOrderByCreatedAtDesc(UUID tenantId, UUID companyId);
}
