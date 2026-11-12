package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.BundleItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataBundleItemRepository extends JpaRepository<BundleItemJpaEntity, UUID> {
    List<BundleItemJpaEntity> findAllByBundleSkuId(UUID bundleSkuId);
    void deleteAllByBundleSkuId(UUID bundleSkuId);
}

