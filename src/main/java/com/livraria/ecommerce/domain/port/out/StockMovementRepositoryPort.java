package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.stock.StockMovement;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepositoryPort {
    StockMovement save(StockMovement movement);
    List<StockMovement> findAllBySkuId(UUID tenantId, UUID companyId, UUID skuId);
    List<StockMovement> findAllByCompanyId(UUID tenantId, UUID companyId, int page, int size);
}

