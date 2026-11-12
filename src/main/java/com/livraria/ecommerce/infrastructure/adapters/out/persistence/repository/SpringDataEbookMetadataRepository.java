package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.EbookMetadataJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataEbookMetadataRepository extends JpaRepository<EbookMetadataJpaEntity, UUID> {
    Optional<EbookMetadataJpaEntity> findBySkuId(UUID skuId);
    void deleteBySkuId(UUID skuId);
}

