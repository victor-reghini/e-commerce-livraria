package com.livraria.ecommerce.domain.model.stock;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EventDashboard(
        UUID companyId,
        String companyName,
        int totalItemsDispatched,
        BigDecimal totalValueDispatched,
        int totalItemsSold,
        BigDecimal totalRevenue,
        int currentPhysicalStock,
        int activeAlertsCount,
        List<TopSellingItem> topSellingItems,
        List<StockAlertItem> lowStockAlerts
) {
    public record TopSellingItem(
            UUID skuId,
            String skuCode,
            String title,
            int quantitySold,
            BigDecimal revenue
    ) {}

    public record StockAlertItem(
            UUID skuId,
            String skuCode,
            String title,
            int currentStock,
            int minThreshold
    ) {}
}

