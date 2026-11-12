package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.catalog.Author;
import com.livraria.ecommerce.domain.model.catalog.AuthorRole;
import com.livraria.ecommerce.domain.model.catalog.BindingType;
import com.livraria.ecommerce.domain.model.catalog.BundleItem;
import com.livraria.ecommerce.domain.model.catalog.Category;
import com.livraria.ecommerce.domain.model.catalog.Dimensions;
import com.livraria.ecommerce.domain.model.catalog.FiscalDecompositionItem;
import com.livraria.ecommerce.domain.model.catalog.IsbnValidator;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.ProductAuthor;
import com.livraria.ecommerce.domain.model.catalog.ProductType;
import com.livraria.ecommerce.domain.model.catalog.Publisher;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import com.livraria.ecommerce.domain.port.in.CatalogUseCase;
import com.livraria.ecommerce.domain.port.out.AuthorRepositoryPort;
import com.livraria.ecommerce.domain.port.out.CategoryRepositoryPort;
import com.livraria.ecommerce.domain.port.out.ProductRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PublisherRepositoryPort;
import com.livraria.ecommerce.domain.port.out.SkuRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CatalogService implements CatalogUseCase {

    private final ProductRepositoryPort productRepository;
    private final SkuRepositoryPort skuRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final AuthorRepositoryPort authorRepository;
    private final PublisherRepositoryPort publisherRepository;
    private final StockRepositoryPort stockRepository;
    private final StoragePort storagePort;

    @Value("${aws.s3.bucket-media:ecommerce-media}")
    private String mediaBucket;

    @Value("${aws.s3.bucket-ebooks:ecommerce-ebooks}")
    private String ebooksBucket;

    public CatalogService(ProductRepositoryPort productRepository,
                          SkuRepositoryPort skuRepository,
                          CategoryRepositoryPort categoryRepository,
                          AuthorRepositoryPort authorRepository,
                          PublisherRepositoryPort publisherRepository,
                          StockRepositoryPort stockRepository,
                          StoragePort storagePort) {
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.categoryRepository = categoryRepository;
        this.authorRepository = authorRepository;
        this.publisherRepository = publisherRepository;
        this.stockRepository = stockRepository;
        this.storagePort = storagePort;
    }

    @Override
    @Transactional
    public Product createProduct(CatalogDto.CreateProductRequest request) {
        String slug = (request.slug() != null && !request.slug().isBlank())
                ? request.slug().trim()
                : Product.generateSlug(request.title());

        if (productRepository.findBySlug(request.tenantId(), slug).isPresent()) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Product product = Product.create(
                request.tenantId(),
                request.categoryId(),
                request.publisherId(),
                request.title(),
                request.subtitle(),
                slug,
                request.synopsis(),
                request.productType()
        );

        if (request.authors() != null) {
            for (CatalogDto.AuthorAssignmentRequest authorReq : request.authors()) {
                authorRepository.findById(authorReq.authorId()).ifPresent(author ->
                        product.addAuthor(author.getId(), author.getName(),
                                authorReq.role() != null ? authorReq.role() : AuthorRole.AUTHOR,
                                authorReq.displayOrder())
                );
            }
        }

        Product savedProduct = productRepository.save(product);

        if (request.initialSkus() != null && !request.initialSkus().isEmpty()) {
            for (CatalogDto.CreateSkuRequest skuReq : request.initialSkus()) {
                addSku(savedProduct.getId(), skuReq);
            }
            return productRepository.findById(savedProduct.getId()).orElse(savedProduct);
        }

        return savedProduct;
    }

    @Override
    @Transactional
    public Product updateProduct(UUID productId, CatalogDto.UpdateProductRequest request) {
        Product product = getProductById(productId);

        product.update(
                request.title(),
                request.subtitle(),
                request.slug(),
                request.synopsis(),
                request.categoryId(),
                request.publisherId(),
                request.productType(),
                request.active()
        );

        if (request.authors() != null) {
            List<ProductAuthor> authors = new ArrayList<>();
            for (CatalogDto.AuthorAssignmentRequest aReq : request.authors()) {
                authorRepository.findById(aReq.authorId()).ifPresent(author ->
                        authors.add(new ProductAuthor(product.getId(), author.getId(), author.getName(),
                                aReq.role() != null ? aReq.role() : AuthorRole.AUTHOR, aReq.displayOrder()))
                );
            }
            product.setAuthors(authors);
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductById(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produto", productId));
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductBySlug(UUID tenantId, String slug) {
        return productRepository.findBySlug(tenantId, slug)
                .orElseThrow(() -> new BusinessException("Produto não encontrado para o slug: " + slug));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> searchProducts(UUID tenantId, String query, UUID categoryId, UUID publisherId, Boolean active, int page, int size) {
        return productRepository.search(tenantId, query, categoryId, publisherId, active, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProducts(UUID tenantId, String query, UUID categoryId, UUID publisherId, Boolean active) {
        return productRepository.countSearch(tenantId, query, categoryId, publisherId, active);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID productId) {
        getProductById(productId);
        productRepository.deleteById(productId);
    }

    @Override
    @Transactional
    public Sku addSku(UUID productId, CatalogDto.CreateSkuRequest request) {
        Product product = getProductById(productId);

        if (skuRepository.findBySkuCode(product.getTenantId(), request.skuCode()).isPresent()) {
            throw new BusinessException("Já existe uma variação com o SKU: " + request.skuCode());
        }

        if (request.isbn() != null && !request.isbn().isBlank()) {
            String cleanedIsbn = IsbnValidator.clean(request.isbn());
            if (!IsbnValidator.isValid(cleanedIsbn)) {
                throw new BusinessException("O ISBN informado (" + request.isbn() + ") é inválido.");
            }
        }

        Dimensions dimensions = new Dimensions(
                request.height() != null ? request.height() : 0.0,
                request.width() != null ? request.width() : 0.0,
                request.depth() != null ? request.depth() : 0.0
        );

        List<BundleItem> bundleItems = new ArrayList<>();
        if (request.bundleItems() != null && !request.bundleItems().isEmpty()) {
            for (CatalogDto.BundleItemRequest bReq : request.bundleItems()) {
                Sku itemSku = skuRepository.findById(bReq.itemSkuId())
                        .orElseThrow(() -> new BusinessException("Item constituinte do kit não encontrado: " + bReq.itemSkuId()));

                bundleItems.add(new BundleItem(
                        UUID.randomUUID(),
                        product.getTenantId(),
                        null,
                        itemSku.getId(),
                        itemSku.getSkuCode(),
                        "Componente " + itemSku.getSkuCode(),
                        itemSku.getPrice(),
                        itemSku.getNcm(),
                        itemSku.getCfop(),
                        bReq.quantity(),
                        bReq.bundleType(),
                        bReq.discountPercentage()
                ));
            }
        }

        Sku sku = new Sku(
                UUID.randomUUID(),
                product.getTenantId(),
                product.getId(),
                request.skuCode().trim(),
                request.barcode(),
                request.isbn(),
                request.bindingType() != null ? request.bindingType() : BindingType.BROCHURA,
                request.editionYear(),
                request.volume(),
                request.pages(),
                request.catalogingSheetText(),
                request.weightGrams(),
                dimensions,
                request.price(),
                request.promotionalPrice(),
                request.costPrice() != null ? request.costPrice() : BigDecimal.ZERO,
                request.ncm() != null ? request.ncm() : "4901.99.00",
                request.cfop() != null ? request.cfop() : "5102",
                true,
                bundleItems,
                null,
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );

        Sku saved = skuRepository.save(sku);
        product.addSku(saved);
        return saved;
    }

    @Override
    @Transactional
    public Sku updateSku(UUID skuId, CatalogDto.UpdateSkuRequest request) {
        Sku sku = getSkuById(skuId);

        Dimensions dimensions = new Dimensions(
                request.height() != null ? request.height() : sku.getDimensions().height(),
                request.width() != null ? request.width() : sku.getDimensions().width(),
                request.depth() != null ? request.depth() : sku.getDimensions().depth()
        );

        if (request.bundleItems() != null) {
            List<BundleItem> bundleItems = new ArrayList<>();
            for (CatalogDto.BundleItemRequest bReq : request.bundleItems()) {
                Sku itemSku = skuRepository.findById(bReq.itemSkuId())
                        .orElseThrow(() -> new BusinessException("Item constituinte do kit não encontrado: " + bReq.itemSkuId()));

                bundleItems.add(new BundleItem(
                        UUID.randomUUID(),
                        sku.getTenantId(),
                        sku.getId(),
                        itemSku.getId(),
                        itemSku.getSkuCode(),
                        "Componente " + itemSku.getSkuCode(),
                        itemSku.getPrice(),
                        itemSku.getNcm(),
                        itemSku.getCfop(),
                        bReq.quantity(),
                        bReq.bundleType(),
                        bReq.discountPercentage()
                ));
            }
            sku.setBundleItems(bundleItems);
        }

        sku.updateDetails(
                request.barcode(),
                request.isbn(),
                request.bindingType(),
                request.editionYear(),
                request.volume(),
                request.pages(),
                request.catalogingSheetText(),
                request.weightGrams(),
                dimensions,
                request.price(),
                request.promotionalPrice(),
                request.costPrice(),
                request.ncm(),
                request.cfop(),
                request.active()
        );

        return skuRepository.save(sku);
    }

    @Override
    @Transactional(readOnly = true)
    public Sku getSkuById(UUID skuId) {
        return skuRepository.findById(skuId)
                .orElseThrow(() -> new EntityNotFoundException("Variação (SKU)", skuId));
    }

    @Override
    @Transactional(readOnly = true)
    public Sku getSkuByBarcode(UUID tenantId, String barcode) {
        return skuRepository.findByBarcode(tenantId, barcode)
                .orElseThrow(() -> new BusinessException("Variação não encontrada para o código de barras: " + barcode));
    }

    @Override
    @Transactional(readOnly = true)
    public Sku getSkuByIsbn(UUID tenantId, String isbn) {
        String cleaned = IsbnValidator.clean(isbn);
        return skuRepository.findByIsbn(tenantId, cleaned)
                .orElseThrow(() -> new BusinessException("Variação não encontrada para o ISBN: " + isbn));
    }

    @Override
    @Transactional
    public void deleteSku(UUID skuId) {
        getSkuById(skuId);
        skuRepository.deleteById(skuId);
    }

    @Override
    @Transactional
    public Category createCategory(UUID tenantId, CatalogDto.CategoryRequest request) {
        Category category = Category.create(tenantId, request.name(), request.slug(), request.parentId());
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> listCategories(UUID tenantId) {
        return categoryRepository.findAllByTenantId(tenantId);
    }

    @Override
    @Transactional
    public Author createAuthor(UUID tenantId, CatalogDto.AuthorRequest request) {
        Author author = Author.create(tenantId, request.name(), request.biography());
        return authorRepository.save(author);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Author> listAuthors(UUID tenantId) {
        return authorRepository.findAllByTenantId(tenantId);
    }

    @Override
    @Transactional
    public Publisher createPublisher(UUID tenantId, CatalogDto.PublisherRequest request) {
        Publisher publisher = Publisher.create(tenantId, request.name());
        return publisherRepository.save(publisher);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publisher> listPublishers(UUID tenantId) {
        return publisherRepository.findAllByTenantId(tenantId);
    }

    @Override
    public CatalogDto.PresignedUrlResponse generateCoverUploadUrl(UUID tenantId, UUID productId, String fileName, String contentType) {
        String key = "tenants/" + tenantId + "/products/" + productId + "/covers/" + UUID.randomUUID() + "-" + fileName;
        Duration duration = Duration.ofMinutes(15);
        String uploadUrl = storagePort.generatePresignedUploadUrl(mediaBucket, key, duration);
        return new CatalogDto.PresignedUrlResponse(uploadUrl, key, duration.toSeconds());
    }

    @Override
    public CatalogDto.PresignedUrlResponse generateEbookUploadUrl(UUID tenantId, UUID skuId, String fileName, String contentType) {
        String key = "tenants/" + tenantId + "/ebooks/" + skuId + "/" + UUID.randomUUID() + "-" + fileName;
        Duration duration = Duration.ofMinutes(15);
        String uploadUrl = storagePort.generatePresignedUploadUrl(ebooksBucket, key, duration);
        return new CatalogDto.PresignedUrlResponse(uploadUrl, key, duration.toSeconds());
    }

    @Override
    public String generateEbookDownloadUrl(UUID tenantId, UUID skuId) {
        Sku sku = getSkuById(skuId);
        if (sku.getEbookMetadata() == null) {
            throw new BusinessException("Esta variação não possui arquivo digital / e-book associado.");
        }
        return storagePort.generatePresignedDownloadUrl(ebooksBucket, sku.getEbookMetadata().getFileS3Key(), Duration.ofMinutes(15));
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateAvailableStock(UUID tenantId, UUID companyId, UUID skuId) {
        Sku sku = getSkuById(skuId);
        if (!sku.isBundle()) {
            return stockRepository.getAvailableStock(tenantId, companyId, skuId);
        }

        List<UUID> componentIds = sku.getBundleItems().stream().map(BundleItem::getItemSkuId).toList();
        Map<UUID, Integer> stockMap = stockRepository.findAvailableStockBySkuIds(tenantId, companyId, componentIds);
        return sku.calculateVirtualStock(stockMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiscalDecompositionItem> decomposeBundle(UUID skuId, BigDecimal invoicedTotal) {
        Sku sku = getSkuById(skuId);
        return sku.decomposeBundleForFiscal(invoicedTotal);
    }
}

