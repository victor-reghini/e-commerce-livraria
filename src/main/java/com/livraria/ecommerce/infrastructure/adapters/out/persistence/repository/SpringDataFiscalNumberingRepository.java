package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.FiscalNumberingJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataFiscalNumberingRepository extends JpaRepository<FiscalNumberingJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FiscalNumberingJpaEntity f WHERE f.tenantId = :tenantId AND f.companyId = :companyId AND f.model = :model AND f.series = :series")
    Optional<FiscalNumberingJpaEntity> findByTenantIdAndCompanyIdAndModelAndSeriesForUpdate(
            @Param("tenantId") UUID tenantId,
            @Param("companyId") UUID companyId,
            @Param("model") String model,
            @Param("series") int series
    );
}

