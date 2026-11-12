package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;
import com.livraria.ecommerce.application.service.CatalogService;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.catalog.Author;
import com.livraria.ecommerce.domain.model.catalog.BindingType;
import com.livraria.ecommerce.domain.model.catalog.BundleItem;
import com.livraria.ecommerce.domain.model.catalog.BundleType;
import com.livraria.ecommerce.domain.model.catalog.Category;
import com.livraria.ecommerce.domain.model.catalog.Dimensions;
import com.livraria.ecommerce.domain.model.catalog.EbookMetadata;
import com.livraria.ecommerce.domain.model.catalog.FiscalDecompositionItem;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.ProductType;
import com.livraria.ecommerce.domain.model.catalog.Publisher;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import com.livraria.ecommerce.domain.port.out.AuthorRepositoryPort;
import com.livraria.ecommerce.domain.port.out.CategoryRepositoryPort;
import com.livraria.ecommerce.domain.port.out.ProductRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PublisherRepositoryPort;
import com.livraria.ecommerce.domain.port.out.SkuRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - CatalogService")
class CatalogServiceTest {

    @Mock
    private ProductRepositoryPort productRepository;
    @Mock
    private SkuRepositoryPort skuRepository;
    @Mock
    private CategoryRepositoryPort categoryRepository;
    @Mock
    private AuthorRepositoryPort authorRepository;
    @Mock
    private PublisherRepositoryPort publisherRepository;
    @Mock
    private StockRepositoryPort stockRepository;
    @Mock
    private StoragePort storagePort;

    private CatalogService catalogService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(
                productRepository, skuRepository, categoryRepository,
                authorRepository, publisherRepository, stockRepository, storagePort
        );
        ReflectionTestUtils.setField(catalogService, "mediaBucket", "test-media");
        ReflectionTestUtils.setField(catalogService, "ebooksBucket", "test-ebooks");
    }

    @Test
    void shouldCreateProductSuccessfully() {
        CatalogDto.CreateProductRequest request = new CatalogDto.CreateProductRequest(
                tenantId, "Domain-Driven Design", "Tackling Complexity in Software",
                null, "Livro clássico do Eric Evans", null, null,
                ProductType.BOOK, null, null
        );

        when(productRepository.findBySlug(eq(tenantId), anyString())).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Product product = catalogService.createProduct(request);

        assertNotNull(product);
        assertEquals("Domain-Driven Design", product.getTitle());
        assertEquals("domain-driven-design", product.getSlug());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldUpdateProductSuccessfully() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(tenantId, null, null, "Old Title", null, null, null, ProductType.BOOK);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogDto.UpdateProductRequest request = new CatalogDto.UpdateProductRequest(
                "New Title", "New Subtitle", "new-title", "Synopsis", null, null, ProductType.BOOK, true, null
        );

        Product updated = catalogService.updateProduct(productId, request);

        assertEquals("New Title", updated.getTitle());
        assertEquals("new-title", updated.getSlug());
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> catalogService.getProductById(id));
    }

    @Test
    void shouldAddSkuSuccessfully() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(tenantId, null, null, "Livro Teste", null, null, null, ProductType.BOOK);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findBySkuCode(tenantId, "SKU-123")).thenReturn(Optional.empty());
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CatalogDto.CreateSkuRequest request = new CatalogDto.CreateSkuRequest(
                tenantId, "SKU-123", "7891234567890", "978-85-359-0277-8",
                BindingType.BROCHURA, 2024, "Vol 1", 200, "CIP", 350,
                20.0, 14.0, 1.5, new BigDecimal("49.90"), null, new BigDecimal("20.00"),
                "4901.99.00", "5102", null
        );

        Sku sku = catalogService.addSku(productId, request);

        assertNotNull(sku);
        assertEquals("SKU-123", sku.getSkuCode());
        assertEquals("9788535902778", sku.getIsbn());
        verify(skuRepository).save(any(Sku.class));
    }

    @Test
    void shouldRejectAddingSkuWithDuplicateCode() {
        UUID productId = UUID.randomUUID();
        Product product = Product.create(tenantId, null, null, "Livro Teste", null, null, null, ProductType.BOOK);
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findBySkuCode(tenantId, "SKU-DUP")).thenReturn(Optional.of(mock(Sku.class)));

        CatalogDto.CreateSkuRequest request = new CatalogDto.CreateSkuRequest(
                tenantId, "SKU-DUP", null, null, BindingType.BROCHURA, null, null, null, null, 0,
                0.0, 0.0, 0.0, BigDecimal.TEN, null, BigDecimal.ZERO, "4901.99.00", "5102", null
        );

        assertThrows(BusinessException.class, () -> catalogService.addSku(productId, request));
    }

    @Test
    void shouldGenerateCoverUploadUrl() {
        UUID productId = UUID.randomUUID();
        when(storagePort.generatePresignedUploadUrl(eq("test-media"), anyString(), any(Duration.class)))
                .thenReturn("https://storage.test/upload-url");

        CatalogDto.PresignedUrlResponse response = catalogService.generateCoverUploadUrl(
                tenantId, productId, "capa.jpg", "image/jpeg"
        );

        assertNotNull(response);
        assertEquals("https://storage.test/upload-url", response.uploadUrl());
        assertTrue(response.s3Key().contains("covers/"));
    }

    @Test
    void shouldGenerateEbookDownloadUrlForEbookSku() {
        UUID skuId = UUID.randomUUID();
        EbookMetadata metadata = new EbookMetadata(UUID.randomUUID(), tenantId, skuId, "ebooks/key.epub", "EPUB", 2048, true);
        Sku sku = new Sku(
                skuId, tenantId, UUID.randomUUID(), "EBK-01", null, null,
                BindingType.DIGITAL_EBOOK, 2024, null, 300, null, 0,
                Dimensions.zero(), BigDecimal.TEN, null, BigDecimal.ZERO, "4901.99.00", "5102", true,
                Collections.emptyList(), metadata, Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(storagePort.generatePresignedDownloadUrl(eq("test-ebooks"), eq("ebooks/key.epub"), any(Duration.class)))
                .thenReturn("https://storage.test/download-ebook");

        String url = catalogService.generateEbookDownloadUrl(tenantId, skuId);
        assertEquals("https://storage.test/download-ebook", url);
    }

    @Test
    void shouldCalculateAvailableStockForDynamicBundle() {
        UUID bundleSkuId = UUID.randomUUID();
        UUID itemAId = UUID.randomUUID();
        UUID itemBId = UUID.randomUUID();

        BundleItem itemA = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, itemAId, "A", "A",
                BigDecimal.TEN, "4901.99.00", "5102", 2, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);
        BundleItem itemB = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, itemBId, "B", "B",
                BigDecimal.TEN, "4901.99.00", "5102", 1, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);

        Sku bundleSku = new Sku(
                bundleSkuId, tenantId, UUID.randomUUID(), "KIT", null, null,
                BindingType.OUTRO, null, null, null, null, 0,
                Dimensions.zero(), BigDecimal.TEN, null, BigDecimal.ZERO, "4901.99.00", "5102", true,
                List.of(itemA, itemB), null, Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(skuRepository.findById(bundleSkuId)).thenReturn(Optional.of(bundleSku));
        when(stockRepository.findAvailableStockBySkuIds(tenantId, companyId, List.of(itemAId, itemBId)))
                .thenReturn(Map.of(itemAId, 20, itemBId, 6));

        // 20 de A (precisa 2) = 10 kits
        // 6 de B (precisa 1) = 6 kits
        // Saldo disponível = min(10, 6) = 6
        int available = catalogService.calculateAvailableStock(tenantId, companyId, bundleSkuId);
        assertEquals(6, available);
    }

    @Test
    void shouldDecomposeBundle() {
        UUID bundleSkuId = UUID.randomUUID();
        UUID itemAId = UUID.randomUUID();

        BundleItem itemA = new BundleItem(UUID.randomUUID(), tenantId, bundleSkuId, itemAId, "A", "A",
                new BigDecimal("50.00"), "4901.99.00", "5102", 1, BundleType.DYNAMIC_VIRTUAL, BigDecimal.ZERO);

        Sku bundleSku = new Sku(
                bundleSkuId, tenantId, UUID.randomUUID(), "KIT", null, null,
                BindingType.OUTRO, null, null, null, null, 0,
                Dimensions.zero(), new BigDecimal("50.00"), null, BigDecimal.ZERO, "4901.99.00", "5102", true,
                List.of(itemA), null, Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(skuRepository.findById(bundleSkuId)).thenReturn(Optional.of(bundleSku));

        List<FiscalDecompositionItem> items = catalogService.decomposeBundle(bundleSkuId, new BigDecimal("45.00"));
        assertEquals(1, items.size());
        assertEquals(new BigDecimal("45.00"), items.get(0).proportionalTotal());
    }

    @Test
    void shouldCreateTaxonomies() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
        when(authorRepository.save(any(Author.class))).thenAnswer(i -> i.getArgument(0));
        when(publisherRepository.save(any(Publisher.class))).thenAnswer(i -> i.getArgument(0));

        Category cat = catalogService.createCategory(tenantId, new CatalogDto.CategoryRequest("Ficção", "ficcao", null));
        Author author = catalogService.createAuthor(tenantId, new CatalogDto.AuthorRequest("J.R.R. Tolkien", "Autor de fantasia"));
        Publisher pub = catalogService.createPublisher(tenantId, new CatalogDto.PublisherRequest("HarperCollins"));

        assertEquals("Ficção", cat.getName());
        assertEquals("J.R.R. Tolkien", author.getName());
        assertEquals("HarperCollins", pub.getName());
    }
}

