package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.ecommerce.ShippingQuote;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ShippingCalculatorPort {
    List<ShippingQuote> calculate(UUID tenantId, String originZip, String destinationZip,
                                 int totalWeightGrams, double height, double width, double depth,
                                 BigDecimal declaredValue);
}

