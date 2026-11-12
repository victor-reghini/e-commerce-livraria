package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductImageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataProductImageRepository extends JpaRepository<ProductImageJpaEntity, UUID> {
    List<ProductImageJpaEntity> findAllByProductIdOrderByDisplayOrderAsc(UUID productId);
    List<ProductImageJpaEntity> findAllBySkuIdOrderByDisplayOrderAsc(UUID skuId);
    void deleteAllByProductId(UUID productId);
}

