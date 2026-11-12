package com.livraria.ecommerce.domain.model.catalog;

import java.math.BigDecimal;
import java.util.UUID;

public record FiscalDecompositionItem(
        UUID skuId,
        String skuCode,
        String title,
        int quantity,
        BigDecimal originalUnitPrice,
        BigDecimal proportionalUnitPrice,
        BigDecimal proportionalTotal,
        String ncm,
        String cfop
) {
}

