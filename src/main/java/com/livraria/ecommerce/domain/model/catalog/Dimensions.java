package com.livraria.ecommerce.domain.model.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Dimensions(
        double height,
        double width,
        double depth
) {
    public Dimensions {
        if (height < 0 || width < 0 || depth < 0) {
            throw new IllegalArgumentException("As dimensões não podem ser negativas.");
        }
    }

    public static Dimensions zero() {
        return new Dimensions(0.0, 0.0, 0.0);
    }

    public double cubicVolume() {
        return BigDecimal.valueOf(height * width * depth)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

