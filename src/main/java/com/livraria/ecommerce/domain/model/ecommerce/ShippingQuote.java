package com.livraria.ecommerce.domain.model.ecommerce;

import java.math.BigDecimal;

public record ShippingQuote(
        ShippingCarrier carrier,
        ShippingService service,
        String name,
        BigDecimal price,
        int deliveryDays,
        String error
) {
    public static ShippingQuote available(ShippingCarrier carrier, ShippingService service, String name, BigDecimal price, int deliveryDays) {
        return new ShippingQuote(carrier, service, name, price, deliveryDays, null);
    }

    public static ShippingQuote unavailable(ShippingCarrier carrier, ShippingService service, String name, String error) {
        return new ShippingQuote(carrier, service, name, BigDecimal.ZERO, 0, error);
    }

    public boolean isAvailable() {
        return error == null || error.isBlank();
    }
}
