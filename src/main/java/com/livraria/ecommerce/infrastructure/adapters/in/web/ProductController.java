package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;
import com.livraria.ecommerce.domain.model.catalog.FiscalDecompositionItem;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import com.livraria.ecommerce.domain.port.in.BatchImportUseCase;
import com.livraria.ecommerce.domain.port.in.CatalogUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Catálogo de Produtos & Estoque", description = "Endpoints para gestão de produtos, variações (SKU), kits, mídias e importação em lote")
public class ProductController {

    private final CatalogUseCase catalogUseCase;
    private final BatchImportUseCase batchImportUseCase;

    public ProductController(CatalogUseCase catalogUseCase, BatchImportUseCase batchImportUseCase) {
        this.catalogUseCase = catalogUseCase;
        this.batchImportUseCase = batchImportUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Criar novo Produto Pai com metadados gerais e SKUs iniciais opcionais")
    public ResponseEntity<CatalogDto.ProductResponse> createProduct(
            @Valid @RequestBody CatalogDto.CreateProductRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(request.tenantId(), tenantHeader);
        CatalogDto.CreateProductRequest scopedRequest = new CatalogDto.CreateProductRequest(
                tenantId,
                request.title(),
                request.subtitle(),
                request.slug(),
                request.synopsis(),
                request.categoryId(),
                request.publisherId(),
                request.productType(),
                request.authors(),
                request.initialSkus()
        );
        Product product = catalogUseCase.createProduct(scopedRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(toProductResponse(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Atualizar Produto Pai existente")
    public ResponseEntity<CatalogDto.ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody CatalogDto.UpdateProductRequest request) {
        Product updated = catalogUseCase.updateProduct(id, request);
        return ResponseEntity.ok(toProductResponse(updated));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar Produto por ID com todas as variações e autores")
    public ResponseEntity<CatalogDto.ProductResponse> getProductById(@PathVariable UUID id) {
        Product product = catalogUseCase.getProductById(id);
        return ResponseEntity.ok(toProductResponse(product));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Buscar Produto pelo slug legível (SEO / Loja pública)")
    public ResponseEntity<CatalogDto.ProductResponse> getProductBySlug(
            @PathVariable String slug,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        Product product = catalogUseCase.getProductBySlug(tenantId, slug);
        return ResponseEntity.ok(toProductResponse(product));
    }

    @GetMapping
    @Operation(summary = "Listar/Pesquisar Produtos com filtros facetados e paginação")
    public ResponseEntity<List<CatalogDto.ProductResponse>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID publisherId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        List<Product> products = catalogUseCase.searchProducts(tenantId, query, categoryId, publisherId, active, page, size);
        List<CatalogDto.ProductResponse> responses = products.stream()
                .map(this::toProductResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Remover Produto e suas variações associadas")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        catalogUseCase.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/skus")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Adicionar nova variação (SKU) a um produto pai")
    public ResponseEntity<CatalogDto.SkuResponse> addSku(
            @PathVariable UUID id,
            @Valid @RequestBody CatalogDto.CreateSkuRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(request.tenantId(), tenantHeader);
        CatalogDto.CreateSkuRequest scopedRequest = new CatalogDto.CreateSkuRequest(
                tenantId,
                request.skuCode(),
                request.barcode(),
                request.isbn(),
                request.bindingType(),
                request.editionYear(),
                request.volume(),
                request.pages(),
                request.catalogingSheetText(),
                request.weightGrams(),
                request.height(),
                request.width(),
                request.depth(),
                request.price(),
                request.promotionalPrice(),
                request.costPrice(),
                request.ncm(),
                request.cfop(),
                request.bundleItems()
        );
        Sku sku = catalogUseCase.addSku(id, scopedRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSkuResponse(sku, null));
    }

    @PutMapping("/skus/{skuId}")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Atualizar atributos de uma variação (SKU)")
    public ResponseEntity<CatalogDto.SkuResponse> updateSku(
            @PathVariable UUID skuId,
            @Valid @RequestBody CatalogDto.UpdateSkuRequest request) {
        Sku updated = catalogUseCase.updateSku(skuId, request);
        return ResponseEntity.ok(toSkuResponse(updated, null));
    }

    @GetMapping("/skus/{skuId}")
    @Operation(summary = "Buscar SKU por ID")
    public ResponseEntity<CatalogDto.SkuResponse> getSkuById(
            @PathVariable UUID skuId,
            @RequestParam(required = false) UUID companyId) {
        Sku sku = catalogUseCase.getSkuById(skuId);
        Integer stock = (companyId != null) ? catalogUseCase.calculateAvailableStock(sku.getTenantId(), companyId, skuId) : null;
        return ResponseEntity.ok(toSkuResponse(sku, stock));
    }

    @GetMapping("/skus/barcode/{barcode}")
    @Operation(summary = "Buscar SKU por código de barras / EAN-13 (para PDV e bipagem rápida)")
    public ResponseEntity<CatalogDto.SkuResponse> getSkuByBarcode(
            @PathVariable String barcode,
            @RequestParam(required = false) UUID companyId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        Sku sku = catalogUseCase.getSkuByBarcode(tenantId, barcode);
        Integer stock = (companyId != null) ? catalogUseCase.calculateAvailableStock(sku.getTenantId(), companyId, sku.getId()) : null;
        return ResponseEntity.ok(toSkuResponse(sku, stock));
    }

    @GetMapping("/skus/isbn/{isbn}")
    @Operation(summary = "Buscar SKU pelo ISBN (formato ISBN-10 ou ISBN-13)")
    public ResponseEntity<CatalogDto.SkuResponse> getSkuByIsbn(
            @PathVariable String isbn,
            @RequestParam(required = false) UUID companyId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        Sku sku = catalogUseCase.getSkuByIsbn(tenantId, isbn);
        Integer stock = (companyId != null) ? catalogUseCase.calculateAvailableStock(sku.getTenantId(), companyId, sku.getId()) : null;
        return ResponseEntity.ok(toSkuResponse(sku, stock));
    }

    @DeleteMapping("/skus/{skuId}")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Remover uma variação (SKU)")
    public ResponseEntity<Void> deleteSku(@PathVariable UUID skuId) {
        catalogUseCase.deleteSku(skuId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/skus/{skuId}/stock")
    @Operation(summary = "Consultar saldo de estoque disponível (calcula dinamicamente para Kits Virtuais)")
    public ResponseEntity<Integer> getSkuStock(
            @PathVariable UUID skuId,
            @RequestParam UUID companyId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        int stock = catalogUseCase.calculateAvailableStock(tenantId, companyId, skuId);
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/skus/{skuId}/fiscal-decomposition")
    @Operation(summary = "Calcular rateio fiscal proporcional de Kit para emissão de NF-e/NFC-e")
    public ResponseEntity<List<FiscalDecompositionItem>> decomposeKitFiscal(
            @PathVariable UUID skuId,
            @RequestParam(required = false) BigDecimal invoicedTotal) {
        List<FiscalDecompositionItem> items = catalogUseCase.decomposeBundle(skuId, invoicedTotal);
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{id}/upload-cover-url")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Gerar URL pré-assinada do S3 para upload direto de capa de livro")
    public ResponseEntity<CatalogDto.PresignedUrlResponse> generateCoverUploadUrl(
            @PathVariable UUID id,
            @RequestParam String fileName,
            @RequestParam(defaultValue = "image/jpeg") String contentType,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        CatalogDto.PresignedUrlResponse response = catalogUseCase.generateCoverUploadUrl(tenantId, id, fileName, contentType);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/skus/{skuId}/upload-ebook-url")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Gerar URL pré-assinada do S3 para upload seguro do arquivo digital do E-book")
    public ResponseEntity<CatalogDto.PresignedUrlResponse> generateEbookUploadUrl(
            @PathVariable UUID skuId,
            @RequestParam String fileName,
            @RequestParam(defaultValue = "application/epub+zip") String contentType,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        CatalogDto.PresignedUrlResponse response = catalogUseCase.generateEbookUploadUrl(tenantId, skuId, fileName, contentType);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/skus/{skuId}/download-ebook-url")
    @Operation(summary = "Gerar link temporário pré-assinado de download para cliente com compra autorizada")
    public ResponseEntity<String> generateEbookDownloadUrl(
            @PathVariable UUID skuId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        String url = catalogUseCase.generateEbookDownloadUrl(tenantId, skuId);
        return ResponseEntity.ok(url);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Importação em lote de planilha Excel (XLSX) ou CSV em chunks de 500 itens com rollback isolado")
    public ResponseEntity<CatalogDto.BatchImportResult> importSpreadsheet(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "companyId", required = false) UUID companyId,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) throws IOException {
        UUID tenantId = resolveTenantId(null, tenantHeader);
        CatalogDto.BatchImportResult result = batchImportUseCase.importSpreadsheet(
                tenantId, companyId, file.getInputStream(), file.getOriginalFilename()
        );
        return ResponseEntity.ok(result);
    }

    private CatalogDto.ProductResponse toProductResponse(Product product) {
        List<CatalogDto.AuthorResponse> authors = product.getAuthors().stream()
                .map(a -> new CatalogDto.AuthorResponse(a.getAuthorId(), a.getAuthorName(), a.getRole(), a.getDisplayOrder()))
                .toList();

        List<CatalogDto.SkuResponse> skus = product.getSkus().stream()
                .map(s -> toSkuResponse(s, null))
                .toList();

        return new CatalogDto.ProductResponse(
                product.getId(),
                product.getTenantId(),
                product.getTitle(),
                product.getSubtitle(),
                product.getSlug(),
                product.getSynopsis(),
                product.getCategoryId(),
                product.getCategoryName(),
                product.getPublisherId(),
                product.getPublisherName(),
                product.getProductType(),
                product.isActive(),
                authors,
                skus
        );
    }

    private CatalogDto.SkuResponse toSkuResponse(Sku sku, Integer stock) {
        List<CatalogDto.BundleItemResponse> bundleResponses = sku.getBundleItems().stream()
                .map(bi -> new CatalogDto.BundleItemResponse(
                        bi.getItemSkuId(),
                        bi.getItemSkuCode(),
                        bi.getItemTitle(),
                        bi.getItemPrice(),
                        bi.getQuantity(),
                        bi.getBundleType(),
                        bi.getDiscountPercentage()
                ))
                .toList();

        return new CatalogDto.SkuResponse(
                sku.getId(),
                sku.getTenantId(),
                sku.getProductId(),
                sku.getSkuCode(),
                sku.getBarcode(),
                sku.getIsbn(),
                sku.getBindingType(),
                sku.getEditionYear(),
                sku.getVolume(),
                sku.getPages(),
                sku.getCatalogingSheetText(),
                sku.getWeightGrams(),
                sku.getDimensions(),
                sku.getPrice(),
                sku.getPromotionalPrice(),
                sku.getCostPrice(),
                sku.getNcm(),
                sku.getCfop(),
                sku.isActive(),
                sku.isBundle(),
                sku.isEbook(),
                bundleResponses,
                stock
        );
    }

    private UUID resolveTenantId(UUID explicitTenantId, String tenantHeader) {
        if (explicitTenantId != null) {
            return explicitTenantId;
        }
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                return UUID.fromString(tenantHeader);
            } catch (IllegalArgumentException ignored) {}
        }
        return TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }
}
