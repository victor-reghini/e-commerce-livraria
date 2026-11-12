package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataStockItemRepository extends JpaRepository<StockItemJpaEntity, UUID> {
    Optional<StockItemJpaEntity> findByTenantIdAndCompanyIdAndSkuId(UUID tenantId, UUID companyId, UUID skuId);
    List<StockItemJpaEntity> findAllByTenantIdAndCompanyIdAndSkuIdIn(UUID tenantId, UUID companyId, List<UUID> skuIds);
}

