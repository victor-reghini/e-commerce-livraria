package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CashShiftJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCashShiftRepository extends JpaRepository<CashShiftJpaEntity, UUID> {

    Optional<CashShiftJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<CashShiftJpaEntity> findByTenantIdAndCompanyIdAndCashierIdAndStatus(
            UUID tenantId, UUID companyId, UUID cashierId, String status
    );

    List<CashShiftJpaEntity> findAllByTenantIdAndCompanyIdOrderByOpenedAtDesc(
            UUID tenantId, UUID companyId, Pageable pageable
    );
}
