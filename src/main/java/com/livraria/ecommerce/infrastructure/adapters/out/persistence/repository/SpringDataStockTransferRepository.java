package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockTransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataStockTransferRepository extends JpaRepository<StockTransferJpaEntity, UUID> {

    Optional<StockTransferJpaEntity> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<StockTransferJpaEntity> findByTenantIdAndTransferNumber(UUID tenantId, String transferNumber);

    @Query("SELECT t FROM StockTransferJpaEntity t WHERE t.tenantId = :tenantId AND (t.originCompanyId = :companyId OR t.destinationCompanyId = :companyId) ORDER BY t.createdAt DESC")
    List<StockTransferJpaEntity> findAllByCompany(@Param("tenantId") UUID tenantId, @Param("companyId") UUID companyId);

    List<StockTransferJpaEntity> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}

