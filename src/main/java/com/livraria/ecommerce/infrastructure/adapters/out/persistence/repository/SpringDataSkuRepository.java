package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSkuRepository extends JpaRepository<SkuJpaEntity, UUID> {
    Optional<SkuJpaEntity> findByTenantIdAndSkuCode(UUID tenantId, String skuCode);
    Optional<SkuJpaEntity> findByTenantIdAndBarcode(UUID tenantId, String barcode);
    Optional<SkuJpaEntity> findByTenantIdAndIsbn(UUID tenantId, String isbn);
    List<SkuJpaEntity> findAllByProductId(UUID productId);
    List<SkuJpaEntity> findAllByIdIn(List<UUID> ids);
}

