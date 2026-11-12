package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import com.livraria.ecommerce.domain.port.in.CatalogUseCase;
import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/store")
@Tag(name = "Loja Pública (Storefront)", description = "Endpoints públicos otimizados para SSR (Next.js), PDP e Busca com Autocomplete")
public class StorefrontController {

    private final CatalogUseCase catalogUseCase;
    private final StockRepositoryPort stockRepository;
    private final SpringDataCompanyRepository companyRepository;

    public StorefrontController(CatalogUseCase catalogUseCase,
                                StockRepositoryPort stockRepository,
                                SpringDataCompanyRepository companyRepository) {
        this.catalogUseCase = catalogUseCase;
        this.stockRepository = stockRepository;
        this.companyRepository = companyRepository;
    }

    private UUID getTenantId(String headerTenantId) {
        if (headerTenantId != null && !headerTenantId.isBlank()) {
            return UUID.fromString(headerTenantId);
        }
        return TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @GetMapping("/products")
    @Operation(summary = "Listagem pública de produtos com paginação e filtros facetados")
    public ResponseEntity<List<CatalogDto.ProductResponse>> listProducts(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID publisherId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        UUID tenantId = getTenantId(tenantIdHeader);
        List<Product> products = catalogUseCase.searchProducts(tenantId, query, categoryId, publisherId, true, page, size);
        return ResponseEntity.ok(products.stream().map(p -> toProductResponse(p, tenantId)).toList());
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "PDP - Página de Detalhe do Produto por slug com variações SKUs e disponibilidade")
    public ResponseEntity<CatalogDto.ProductResponse> getProductBySlug(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable String slug) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Product product = catalogUseCase.getProductBySlug(tenantId, slug);
        return ResponseEntity.ok(toProductResponse(product, tenantId));
    }

    @GetMapping("/search")
    @Operation(summary = "Busca textual rápida de produtos com autocomplete para a barra de pesquisa")
    public ResponseEntity<List<CatalogDto.ProductResponse>> search(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "10") int limit) {
        UUID tenantId = getTenantId(tenantIdHeader);
        List<Product> products = catalogUseCase.searchProducts(tenantId, q, null, null, true, 0, limit);
        return ResponseEntity.ok(products.stream().map(p -> toProductResponse(p, tenantId)).toList());
    }

    private CatalogDto.ProductResponse toProductResponse(Product product, UUID tenantId) {
        UUID companyId = companyRepository.findAllByTenantId(tenantId).stream()
                .filter(CompanyJpaEntity::isHeadquarters)
                .findFirst()
                .map(CompanyJpaEntity::getId)
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        List<CatalogDto.SkuResponse> skuResponses = product.getSkus().stream().map(s -> {
            int stock = catalogUseCase.calculateAvailableStock(tenantId, companyId, s.getId());
            List<CatalogDto.BundleItemResponse> bundleResponses = s.getBundleItems().stream()
                    .map(bi -> new CatalogDto.BundleItemResponse(
                            bi.getItemSkuId(),
                            bi.getItemSkuCode(),
                            bi.getItemTitle(),
                            bi.getItemPrice(),
                            bi.getQuantity(),
                            bi.getBundleType(),
                            bi.getDiscountPercentage()
                    )).toList();

            return new CatalogDto.SkuResponse(
                    s.getId(),
                    s.getTenantId(),
                    s.getProductId(),
                    s.getSkuCode(),
                    s.getBarcode(),
                    s.getIsbn(),
                    s.getBindingType(),
                    s.getEditionYear(),
                    s.getVolume(),
                    s.getPages(),
                    s.getCatalogingSheetText(),
                    s.getWeightGrams(),
                    s.getDimensions(),
                    s.getPrice(),
                    s.getPromotionalPrice(),
                    s.getCostPrice(),
                    s.getNcm(),
                    s.getCfop(),
                    s.isActive(),
                    s.isBundle(),
                    s.isEbook(),
                    bundleResponses,
                    stock
            );
        }).toList();

        List<CatalogDto.AuthorResponse> authorResponses = product.getAuthors().stream()
                .map(a -> new CatalogDto.AuthorResponse(
                        a.getAuthorId(),
                        a.getAuthorName(),
                        a.getRole(),
                        a.getDisplayOrder()
                )).toList();

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
                authorResponses,
                skuResponses
        );
    }
}
