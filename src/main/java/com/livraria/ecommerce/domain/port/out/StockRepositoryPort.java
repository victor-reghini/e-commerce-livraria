package com.livraria.ecommerce.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StockRepositoryPort {
    Map<UUID, Integer> findAvailableStockBySkuIds(UUID tenantId, UUID companyId, List<UUID> skuIds);
    int getAvailableStock(UUID tenantId, UUID companyId, UUID skuId);
    void setStockQuantity(UUID tenantId, UUID companyId, UUID skuId, int quantity);
}

