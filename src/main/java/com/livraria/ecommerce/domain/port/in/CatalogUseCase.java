package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;
import com.livraria.ecommerce.domain.model.catalog.Author;
import com.livraria.ecommerce.domain.model.catalog.Category;
import com.livraria.ecommerce.domain.model.catalog.FiscalDecompositionItem;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.Publisher;
import com.livraria.ecommerce.domain.model.catalog.Sku;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CatalogUseCase {

    Product createProduct(CatalogDto.CreateProductRequest request);

    Product updateProduct(UUID productId, CatalogDto.UpdateProductRequest request);

    Product getProductById(UUID productId);

    Product getProductBySlug(UUID tenantId, String slug);

    List<Product> searchProducts(UUID tenantId, String query, UUID categoryId, UUID publisherId, Boolean active, int page, int size);

    long countProducts(UUID tenantId, String query, UUID categoryId, UUID publisherId, Boolean active);

    void deleteProduct(UUID productId);

    Sku addSku(UUID productId, CatalogDto.CreateSkuRequest request);

    Sku updateSku(UUID skuId, CatalogDto.UpdateSkuRequest request);

    Sku getSkuById(UUID skuId);

    Sku getSkuByBarcode(UUID tenantId, String barcode);

    Sku getSkuByIsbn(UUID tenantId, String isbn);

    void deleteSku(UUID skuId);

    Category createCategory(UUID tenantId, CatalogDto.CategoryRequest request);

    List<Category> listCategories(UUID tenantId);

    Author createAuthor(UUID tenantId, CatalogDto.AuthorRequest request);

    List<Author> listAuthors(UUID tenantId);

    Publisher createPublisher(UUID tenantId, CatalogDto.PublisherRequest request);

    List<Publisher> listPublishers(UUID tenantId);

    CatalogDto.PresignedUrlResponse generateCoverUploadUrl(UUID tenantId, UUID productId, String fileName, String contentType);

    CatalogDto.PresignedUrlResponse generateEbookUploadUrl(UUID tenantId, UUID skuId, String fileName, String contentType);

    String generateEbookDownloadUrl(UUID tenantId, UUID skuId);

    int calculateAvailableStock(UUID tenantId, UUID companyId, UUID skuId);

    List<FiscalDecompositionItem> decomposeBundle(UUID skuId, BigDecimal invoicedTotal);
}

