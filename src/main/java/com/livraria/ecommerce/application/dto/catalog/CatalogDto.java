package com.livraria.ecommerce.application.dto.catalog;

import com.livraria.ecommerce.domain.model.catalog.AuthorRole;
import com.livraria.ecommerce.domain.model.catalog.BindingType;
import com.livraria.ecommerce.domain.model.catalog.BundleType;
import com.livraria.ecommerce.domain.model.catalog.Dimensions;
import com.livraria.ecommerce.domain.model.catalog.ProductType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CatalogDto {

    public record CreateProductRequest(
            @NotNull UUID tenantId,
            @NotBlank String title,
            String subtitle,
            String slug,
            String synopsis,
            UUID categoryId,
            UUID publisherId,
            ProductType productType,
            List<AuthorAssignmentRequest> authors,
            List<CreateSkuRequest> initialSkus
    ) {}

    public record UpdateProductRequest(
            @NotBlank String title,
            String subtitle,
            String slug,
            String synopsis,
            UUID categoryId,
            UUID publisherId,
            ProductType productType,
            boolean active,
            List<AuthorAssignmentRequest> authors
    ) {}

    public record AuthorAssignmentRequest(
            @NotNull UUID authorId,
            AuthorRole role,
            int displayOrder
    ) {}

    public record CreateSkuRequest(
            @NotNull UUID tenantId,
            @NotBlank String skuCode,
            String barcode,
            String isbn,
            BindingType bindingType,
            Integer editionYear,
            String volume,
            Integer pages,
            String catalogingSheetText,
            int weightGrams,
            Double height,
            Double width,
            Double depth,
            @NotNull @PositiveOrZero BigDecimal price,
            BigDecimal promotionalPrice,
            BigDecimal costPrice,
            String ncm,
            String cfop,
            List<BundleItemRequest> bundleItems
    ) {}

    public record UpdateSkuRequest(
            String barcode,
            String isbn,
            BindingType bindingType,
            Integer editionYear,
            String volume,
            Integer pages,
            String catalogingSheetText,
            int weightGrams,
            Double height,
            Double width,
            Double depth,
            @NotNull @PositiveOrZero BigDecimal price,
            BigDecimal promotionalPrice,
            BigDecimal costPrice,
            String ncm,
            String cfop,
            boolean active,
            List<BundleItemRequest> bundleItems
    ) {}

    public record BundleItemRequest(
            @NotNull UUID itemSkuId,
            int quantity,
            BundleType bundleType,
            BigDecimal discountPercentage
    ) {}

    public record CategoryRequest(
            @NotBlank String name,
            String slug,
            UUID parentId
    ) {}

    public record AuthorRequest(
            @NotBlank String name,
            String biography
    ) {}

    public record PublisherRequest(
            @NotBlank String name
    ) {}

    public record PresignedUrlResponse(
            String uploadUrl,
            String s3Key,
            long expiresInSeconds
    ) {}

    public record ProductResponse(
            UUID id,
            UUID tenantId,
            String title,
            String subtitle,
            String slug,
            String synopsis,
            UUID categoryId,
            String categoryName,
            UUID publisherId,
            String publisherName,
            ProductType productType,
            boolean active,
            List<AuthorResponse> authors,
            List<SkuResponse> skus
    ) {}

    public record AuthorResponse(
            UUID authorId,
            String authorName,
            AuthorRole role,
            int displayOrder
    ) {}

    public record SkuResponse(
            UUID id,
            UUID tenantId,
            UUID productId,
            String skuCode,
            String barcode,
            String isbn,
            BindingType bindingType,
            Integer editionYear,
            String volume,
            Integer pages,
            String catalogingSheetText,
            int weightGrams,
            Dimensions dimensions,
            BigDecimal price,
            BigDecimal promotionalPrice,
            BigDecimal costPrice,
            String ncm,
            String cfop,
            boolean active,
            boolean isBundle,
            boolean isEbook,
            List<BundleItemResponse> bundleItems,
            Integer availableStock
    ) {}

    public record BundleItemResponse(
            UUID itemSkuId,
            String itemSkuCode,
            String itemTitle,
            BigDecimal itemPrice,
            int quantity,
            BundleType bundleType,
            BigDecimal discountPercentage
    ) {}

    public record BatchImportRowError(
            int rowNumber,
            String skuCode,
            String title,
            String errorMessage
    ) {}

    public record BatchImportResult(
            int totalProcessed,
            int successCount,
            int failureCount,
            List<BatchImportRowError> errors,
            long executionTimeMs
    ) {}
}

