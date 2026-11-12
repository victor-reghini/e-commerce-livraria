package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.catalog.BindingType;
import com.livraria.ecommerce.domain.model.catalog.BundleItem;
import com.livraria.ecommerce.domain.model.catalog.BundleType;
import com.livraria.ecommerce.domain.model.catalog.Dimensions;
import com.livraria.ecommerce.domain.model.catalog.EbookMetadata;
import com.livraria.ecommerce.domain.model.catalog.FiscalDecompositionItem;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - Sku (Domínio, Estoque Virtual e Decomposição Fiscal de Kits)")
class SkuTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @Test
    void shouldCreateSimpleSkuSuccessfully() {
        Sku sku = new Sku(
                UUID.randomUUID(), tenantId, productId, "LIV-001", "7891234567890", "9788535902778",
                BindingType.BROCHURA, 2024, "Vol 1", 320, "Ficha CIP", 450,
                new Dimensions(21.0, 14.0, 2.0), new BigDecimal("59.90"), new BigDecimal("49.90"),
                new BigDecimal("25.00"), "4901.99.00", "5102", true,
                Collections.emptyList(), null, Collections.emptyList(), Instant.now(), Instant.now()
        );

        assertNotNull(sku.getId());
        assertEquals("LIV-001", sku.getSkuCode());
        assertEquals("9788535902778", sku.getIsbn());
        assertEquals(new BigDecimal("49.90"), sku.getEffectivePrice());
        assertFalse(sku.isBundle());
        assertFalse(sku.isEbook());
    }

    @Test
    void shouldRejectSkuWithInvalidIsbn() {
        assertThrows(BusinessException.class, () -> new Sku(
                UUID.randomUUID(), tenantId, productId, "LIV-001", null, "978-85-359-0277-9", // dígito errado
                BindingType.BROCHURA, 2024, null, 100, null, 100,
                Dimensions.zero(), BigDecimal.TEN, null, BigDecimal.ONE, "4901.99.00", "5102", true,
                Collections.emptyList(), null, Collections.emptyList(), Instant.now(), Instant.now()
        ));
    }

    @Test
    void shouldIdentifyEbookCorrectly() {
        UUID skuId = UUID.randomUUID();
        EbookMetadata ebook = new EbookMetadata(UUID.randomUUID(), tenantId, skuId, "ebooks/sample.epub", "EPUB", 1048576, true);
        Sku sku = new Sku(
                skuId, tenantId, productId, "EBK-001", null, null,
                BindingType.DIGITAL_EBOOK, 2024, null, 250, null, 0,
                Dimensions.zero(), new BigDecimal("29.90"), null, BigDecimal.ZERO, "4901.99.00", "5102", true,
                Collections.emptyList(), ebook, Collections.emptyList(), Instant.now(), Instant.now()
        );

        assertTrue(sku.isEbook());
        assertNotNull(sku.getEbookMetadata());
        assertTrue(sku.getEbookMetadata().isDrmEnabled());
    }

    @Test
    void shouldCalculateVirtualStockForDynamicBundle() {
        UUID bundleSkuId = UUID.randomUUID();
        UUID itemAId = UUID.randomUUID();
        UUID itemBId = UUID.randomUUID();

        // Kit: 2 unidades de A + 1 unidade de B
        BundleItem itemA = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, itemAId, "ITEM-A",
                "Livro A", new BigDecimal("40.00"), "4901.99.00", "5102", 2, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);
        BundleItem itemB = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, itemBId, "ITEM-B",
                "Livro B", new BigDecimal("50.00"), "4901.99.00", "5102", 1, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);

        Sku bundleSku = new Sku(
                bundleSkuId, tenantId, productId, "KIT-TRILOGIA", null, null,
                BindingType.OUTRO, null, null, null, null, 800,
                new Dimensions(25.0, 18.0, 5.0), new BigDecimal("110.00"), null, BigDecimal.ZERO,
                "4901.99.00", "5102", true,
                List.of(itemA, itemB), null, Collections.emptyList(), Instant.now(), Instant.now()
        );

        assertTrue(bundleSku.isBundle());

        // Estoque: A tem 10 unidades, B tem 4 unidades
        // Com 10 unidades de A (precisa de 2 cada) => até 5 kits
        // Com 4 unidades de B (precisa de 1 cada) => até 4 kits
        // Saldo virtual do kit deve ser min(5, 4) = 4
        Map<UUID, Integer> stockMap = new HashMap<>();
        stockMap.put(itemAId, 10);
        stockMap.put(itemBId, 4);

        assertEquals(4, bundleSku.calculateVirtualStock(stockMap));

        // Se o estoque de B for 0, saldo do kit é 0
        stockMap.put(itemBId, 0);
        assertEquals(0, bundleSku.calculateVirtualStock(stockMap));
    }

    @Test
    void shouldDecomposeBundleFiscalRatesProportionally() {
        UUID bundleSkuId = UUID.randomUUID();
        UUID itemAId = UUID.randomUUID();
        UUID itemBId = UUID.randomUUID();

        // Item A: R$ 60,00 | NCM 4901.99.00
        // Item B: R$ 40,00 | NCM 4901.99.00
        // Total Original = R$ 100,00
        BundleItem itemA = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, itemAId, "ITEM-A",
                "Livro A", new BigDecimal("60.00"), "4901.99.00", "5102", 1, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);
        BundleItem itemB = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, itemBId, "ITEM-B",
                "Livro B", new BigDecimal("40.00"), "4901.99.00", "5102", 1, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);

        // Kit vendido promocionalmente por R$ 80,00 (20% de desconto no combo)
        Sku bundleSku = new Sku(
                bundleSkuId, tenantId, productId, "KIT-PROMO", null, null,
                BindingType.OUTRO, null, null, null, null, 500,
                Dimensions.zero(), new BigDecimal("80.00"), null, BigDecimal.ZERO,
                "4901.99.00", "5102", true,
                List.of(itemA, itemB), null, Collections.emptyList(), Instant.now(), Instant.now()
        );

        List<FiscalDecompositionItem> decomposition = bundleSku.decomposeBundleForFiscal(new BigDecimal("80.00"));

        assertEquals(2, decomposition.size());

        FiscalDecompositionItem docItemA = decomposition.get(0);
        FiscalDecompositionItem docItemB = decomposition.get(1);

        // 60% de R$ 80,00 = R$ 48,00
        assertEquals(new BigDecimal("48.00"), docItemA.proportionalTotal());
        // 40% de R$ 80,00 = R$ 32,00
        assertEquals(new BigDecimal("32.00"), docItemB.proportionalTotal());

        // Soma exata = R$ 80,00
        BigDecimal totalDecomposed = docItemA.proportionalTotal().add(docItemB.proportionalTotal());
        assertEquals(new BigDecimal("80.00"), totalDecomposed);
    }

    @Test
    void shouldReconcileCentRoundingInFiscalDecomposition() {
        UUID bundleSkuId = UUID.randomUUID();
        UUID item1Id = UUID.randomUUID();
        UUID item2Id = UUID.randomUUID();
        UUID item3Id = UUID.randomUUID();

        // 3 itens de mesmo preço original R$ 30,00 cada (Total R$ 90,00)
        // Kit faturado por R$ 100,00 (divisão 100 / 3 dá dízima 33.3333...)
        BundleItem item1 = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, item1Id, "I1", "I1", new BigDecimal("30.00"), "4901.99.00", "5102", 1, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);
        BundleItem item2 = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, item2Id, "I2", "I2", new BigDecimal("30.00"), "4901.99.00", "5102", 1, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);
        BundleItem item3 = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, item3Id, "I3", "I3", new BigDecimal("30.00"), "4901.99.00", "5102", 1, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);

        Sku bundleSku = new Sku(
                bundleSkuId, tenantId, productId, "KIT-3", null, null,
                BindingType.OUTRO, null, null, null, null, 300,
                Dimensions.zero(), new BigDecimal("100.00"), null, BigDecimal.ZERO,
                "4901.99.00", "5102", true,
                List.of(item1, item2, item3), null, Collections.emptyList(), Instant.now(), Instant.now()
        );

        List<FiscalDecompositionItem> decomposition = bundleSku.decomposeBundleForFiscal(new BigDecimal("100.00"));

        BigDecimal sum = decomposition.stream()
                .map(FiscalDecompositionItem::proportionalTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // A soma dos centavos reconciliados DEVE fechar perfeitamente em 100.00
        assertEquals(new BigDecimal("100.00"), sum);
    }
}

