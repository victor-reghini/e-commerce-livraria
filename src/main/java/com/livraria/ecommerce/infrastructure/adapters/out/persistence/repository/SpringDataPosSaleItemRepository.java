package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.PosSaleItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPosSaleItemRepository extends JpaRepository<PosSaleItemJpaEntity, UUID> {
    List<PosSaleItemJpaEntity> findAllBySaleId(UUID saleId);
}
