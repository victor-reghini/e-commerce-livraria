package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.model.catalog.AuthorRole;
import com.livraria.ecommerce.domain.model.catalog.BindingType;
import com.livraria.ecommerce.domain.model.catalog.Dimensions;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.ProductType;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - Product (Domínio)")
class ProductTest {

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void shouldGenerateSlugCorrectlyHandlingAccents() {
        assertEquals("o-senhor-dos-aneis-a-sociedade-do-anel",
                Product.generateSlug("O Senhor dos Anéis: A Sociedade do Anel!"));
        assertEquals("livro-de-ficcao-cientifica",
                Product.generateSlug("Livro de Ficção Científica"));
    }

    @Test
    void shouldCreateProductWithValidData() {
        Product product = Product.create(
                tenantId, null, null,
                "Clean Architecture",
                "A Craftsman's Guide to Software Structure",
                null,
                "Livro seminal de arquitetura de software.",
                ProductType.BOOK
        );

        assertNotNull(product.getId());
        assertEquals("Clean Architecture", product.getTitle());
        assertEquals("clean-architecture", product.getSlug());
        assertTrue(product.isActive());
        assertTrue(product.getAuthors().isEmpty());
        assertTrue(product.getSkus().isEmpty());
    }

    @Test
    void shouldAddAndRemoveAuthorAndSku() {
        Product product = Product.create(tenantId, null, null, "Refactoring", null, null, null, ProductType.BOOK);
        UUID authorId = UUID.randomUUID();

        product.addAuthor(authorId, "Martin Fowler", AuthorRole.AUTHOR, 1);
        assertEquals(1, product.getAuthors().size());
        assertEquals("Martin Fowler", product.getAuthors().get(0).getAuthorName());

        Sku sku = new Sku(
                UUID.randomUUID(), tenantId, product.getId(), "REF-01", "1234567890123", "9788535902778",
                BindingType.BROCHURA, 2019, null, 448, null, 600,
                Dimensions.zero(), new BigDecimal("99.90"), null, BigDecimal.ZERO, "4901.99.00", "5102", true,
                Collections.emptyList(), null, Collections.emptyList(), Instant.now(), Instant.now()
        );

        product.addSku(sku);
        assertEquals(1, product.getSkus().size());
        assertEquals(sku, product.getPrimarySku());

        product.removeSku(sku.getId());
        assertEquals(0, product.getSkus().size());
        assertNull(product.getPrimarySku());
    }

    @Test
    void shouldThrowExceptionWhenTitleIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                Product.create(tenantId, null, null, "   ", null, null, null, ProductType.BOOK));
    }
}

