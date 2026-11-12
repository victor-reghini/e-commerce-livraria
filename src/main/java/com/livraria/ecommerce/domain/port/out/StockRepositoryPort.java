package com.livraria.ecommerce.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface StockRepositoryPort {
    Map<UUID, Integer> findAvailableStockBySkuIds(UUID tenantId, UUID companyId, List<UUID> skuIds);
    int getAvailableStock(UUID tenantId, UUID companyId, UUID skuId);
    int getPhysicalStock(UUID tenantId, UUID companyId, UUID skuId);
    int getTotalPhysicalStock(UUID tenantId, UUID companyId);
    void setStockQuantity(UUID tenantId, UUID companyId, UUID skuId, int quantity);
    void reserveStock(UUID tenantId, UUID companyId, UUID skuId, int quantity);
    void releaseStockReservation(UUID tenantId, UUID companyId, UUID skuId, int quantity);
    void transferStock(UUID tenantId, UUID originCompanyId, UUID destinationCompanyId, UUID skuId, int quantity);
}

