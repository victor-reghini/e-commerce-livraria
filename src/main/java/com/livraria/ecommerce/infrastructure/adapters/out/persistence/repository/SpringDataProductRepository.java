package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataProductRepository extends JpaRepository<ProductJpaEntity, UUID> {

    Optional<ProductJpaEntity> findByTenantIdAndSlug(UUID tenantId, String slug);

    @Query("""
        SELECT p FROM ProductJpaEntity p
        WHERE p.tenantId = :tenantId
          AND (:query IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(p.slug) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:categoryId IS NULL OR p.categoryId = :categoryId)
          AND (:publisherId IS NULL OR p.publisherId = :publisherId)
          AND (:active IS NULL OR p.active = :active)
        ORDER BY p.title ASC
    """)
    Page<ProductJpaEntity> search(
            @Param("tenantId") UUID tenantId,
            @Param("query") String query,
            @Param("categoryId") UUID categoryId,
            @Param("publisherId") UUID publisherId,
            @Param("active") Boolean active,
            Pageable pageable
    );
}

