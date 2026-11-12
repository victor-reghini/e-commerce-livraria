package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.model.catalog.Dimensions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - Dimensions")
class DimensionsTest {

    @Test
    void shouldCalculateCubicVolumeCorrectly() {
        Dimensions dim = new Dimensions(23.0, 16.0, 2.5);
        assertEquals(920.0, dim.cubicVolume());
    }

    @Test
    void shouldCreateZeroDimensions() {
        Dimensions zero = Dimensions.zero();
        assertEquals(0.0, zero.height());
        assertEquals(0.0, zero.width());
        assertEquals(0.0, zero.depth());
        assertEquals(0.0, zero.cubicVolume());
    }

    @Test
    void shouldThrowExceptionForNegativeDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new Dimensions(-1.0, 10.0, 5.0));
        assertThrows(IllegalArgumentException.class, () -> new Dimensions(10.0, -2.0, 5.0));
        assertThrows(IllegalArgumentException.class, () -> new Dimensions(10.0, 10.0, -0.5));
    }
}

