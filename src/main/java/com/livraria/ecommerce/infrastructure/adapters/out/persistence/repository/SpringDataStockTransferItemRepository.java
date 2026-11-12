package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockTransferItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataStockTransferItemRepository extends JpaRepository<StockTransferItemJpaEntity, UUID> {
    List<StockTransferItemJpaEntity> findAllByTransferId(UUID transferId);
}
