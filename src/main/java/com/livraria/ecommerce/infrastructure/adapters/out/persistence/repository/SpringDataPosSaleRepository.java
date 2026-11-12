package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.PosSaleJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPosSaleRepository extends JpaRepository<PosSaleJpaEntity, UUID> {

    Optional<PosSaleJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<PosSaleJpaEntity> findByTenantIdAndClientUuid(UUID tenantId, UUID clientUuid);

    Optional<PosSaleJpaEntity> findByTenantIdAndSaleNumber(UUID tenantId, String saleNumber);

    List<PosSaleJpaEntity> findAllByTenantIdAndShiftIdOrderByCreatedAtDesc(UUID tenantId, UUID shiftId);

    List<PosSaleJpaEntity> findAllByTenantIdAndCompanyIdOrderByCreatedAtDesc(UUID tenantId, UUID companyId, Pageable pageable);
}
