package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductAuthorId;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductAuthorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataProductAuthorRepository extends JpaRepository<ProductAuthorJpaEntity, ProductAuthorId> {
    List<ProductAuthorJpaEntity> findAllByProductIdOrderByDisplayOrderAsc(UUID productId);
    void deleteAllByProductId(UUID productId);
}

