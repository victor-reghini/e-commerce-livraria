package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.domain.model.catalog.BindingType;
import com.livraria.ecommerce.domain.model.catalog.BundleItem;
import com.livraria.ecommerce.domain.model.catalog.BundleType;
import com.livraria.ecommerce.domain.model.catalog.Dimensions;
import com.livraria.ecommerce.domain.model.catalog.EbookMetadata;
import com.livraria.ecommerce.domain.model.catalog.ProductImage;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import com.livraria.ecommerce.domain.port.out.SkuRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.BundleItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.EbookMetadataJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataBundleItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataEbookMetadataRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductImageRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SkuPersistenceAdapter implements SkuRepositoryPort {

    private static final Logger log = LoggerFactory.getLogger(SkuPersistenceAdapter.class);

    private final SpringDataSkuRepository skuRepository;
    private final SpringDataBundleItemRepository bundleItemRepository;
    private final SpringDataEbookMetadataRepository ebookMetadataRepository;
    private final SpringDataProductImageRepository productImageRepository;
    private final ObjectMapper objectMapper;

    public SkuPersistenceAdapter(SpringDataSkuRepository skuRepository,
                                 SpringDataBundleItemRepository bundleItemRepository,
                                 SpringDataEbookMetadataRepository ebookMetadataRepository,
                                 SpringDataProductImageRepository productImageRepository,
                                 ObjectMapper objectMapper) {
        this.skuRepository = skuRepository;
        this.bundleItemRepository = bundleItemRepository;
        this.ebookMetadataRepository = ebookMetadataRepository;
        this.productImageRepository = productImageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Sku save(Sku sku) {
        SkuJpaEntity entity = toEntity(sku);
        SkuJpaEntity saved = skuRepository.save(entity);

        // Bundle items
        bundleItemRepository.deleteAllByBundleSkuId(saved.getId());
        if (sku.isBundle()) {
            List<BundleItemJpaEntity> bundleEntities = sku.getBundleItems().stream()
                    .map(item -> BundleItemJpaEntity.builder()
                            .id(item.getId() != null ? item.getId() : UUID.randomUUID())
                            .tenantId(saved.getTenantId())
                            .bundleSkuId(saved.getId())
                            .itemSkuId(item.getItemSkuId())
                            .quantity(item.getQuantity())
                            .bundleType(item.getBundleType() != null ? item.getBundleType().name() : BundleType.DYNAMIC_VIRTUAL.name())
                            .discountPercentage(item.getDiscountPercentage())
                            .createdAt(Instant.now())
                            .build())
                    .toList();
            bundleItemRepository.saveAll(bundleEntities);
        }

        // Ebook metadata
        if (sku.getEbookMetadata() != null) {
            EbookMetadata em = sku.getEbookMetadata();
            EbookMetadataJpaEntity emEntity = EbookMetadataJpaEntity.builder()
                    .id(em.getId() != null ? em.getId() : UUID.randomUUID())
                    .tenantId(saved.getTenantId())
                    .skuId(saved.getId())
                    .fileS3Key(em.getFileS3Key())
                    .fileFormat(em.getFileFormat())
                    .fileSizeBytes(em.getFileSizeBytes())
                    .drmEnabled(em.isDrmEnabled())
                    .createdAt(Instant.now())
                    .build();
            ebookMetadataRepository.save(emEntity);
        }

        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sku> findById(UUID id) {
        return skuRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sku> findBySkuCode(UUID tenantId, String skuCode) {
        return skuRepository.findByTenantIdAndSkuCode(tenantId, skuCode).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sku> findByBarcode(UUID tenantId, String barcode) {
        return skuRepository.findByTenantIdAndBarcode(tenantId, barcode).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Sku> findByIsbn(UUID tenantId, String isbn) {
        return skuRepository.findByTenantIdAndIsbn(tenantId, isbn).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sku> findAllByProductId(UUID productId) {
        return skuRepository.findAllByProductId(productId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sku> findAllByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return skuRepository.findAllByIdIn(ids).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        bundleItemRepository.deleteAllByBundleSkuId(id);
        ebookMetadataRepository.deleteBySkuId(id);
        skuRepository.deleteById(id);
    }

    private SkuJpaEntity toEntity(Sku sku) {
        String dimensionsJson = "{}";
        try {
            if (sku.getDimensions() != null) {
                dimensionsJson = objectMapper.writeValueAsString(sku.getDimensions());
            }
        } catch (JsonProcessingException e) {
            log.warn("Erro ao serializar dimensões do SKU {}: {}", sku.getSkuCode(), e.getMessage());
        }

        return SkuJpaEntity.builder()
                .id(sku.getId())
                .tenantId(sku.getTenantId())
                .productId(sku.getProductId())
                .skuCode(sku.getSkuCode())
                .barcode(sku.getBarcode())
                .isbn(sku.getIsbn())
                .bindingType(sku.getBindingType() != null ? sku.getBindingType().name() : BindingType.BROCHURA.name())
                .editionYear(sku.getEditionYear())
                .volume(sku.getVolume())
                .pages(sku.getPages())
                .catalogingSheetText(sku.getCatalogingSheetText())
                .weightGrams(sku.getWeightGrams())
                .dimensionsJson(dimensionsJson)
                .price(sku.getPrice())
                .promotionalPrice(sku.getPromotionalPrice())
                .costPrice(sku.getCostPrice())
                .ncm(sku.getNcm())
                .cfop(sku.getCfop())
                .active(sku.isActive())
                .createdAt(sku.getCreatedAt())
                .updatedAt(sku.getUpdatedAt())
                .build();
    }

    private Sku toDomain(SkuJpaEntity entity) {
        Dimensions dimensions = Dimensions.zero();
        if (entity.getDimensionsJson() != null && !entity.getDimensionsJson().isBlank() && !entity.getDimensionsJson().equals("{}")) {
            try {
                dimensions = objectMapper.readValue(entity.getDimensionsJson(), Dimensions.class);
            } catch (Exception e) {
                log.warn("Falha ao desserializar dimensões do SKU {}: {}", entity.getSkuCode(), e.getMessage());
            }
        }

        BindingType bindingType = BindingType.BROCHURA;
        if (entity.getBindingType() != null) {
            try {
                bindingType = BindingType.valueOf(entity.getBindingType());
            } catch (IllegalArgumentException ignored) {}
        }

        // Load bundle items
        List<BundleItemJpaEntity> bundleEntities = bundleItemRepository.findAllByBundleSkuId(entity.getId());
        List<BundleItem> bundleItems = new ArrayList<>();
        if (!bundleEntities.isEmpty()) {
            List<UUID> itemIds = bundleEntities.stream().map(BundleItemJpaEntity::getItemSkuId).toList();
            Map<UUID, SkuJpaEntity> itemsMap = skuRepository.findAllByIdIn(itemIds).stream()
                    .collect(Collectors.toMap(SkuJpaEntity::getId, s -> s));

            for (BundleItemJpaEntity be : bundleEntities) {
                SkuJpaEntity itemSku = itemsMap.get(be.getItemSkuId());
                String code = itemSku != null ? itemSku.getSkuCode() : "N/A";
                bundleItems.add(new BundleItem(
                        be.getId(),
                        be.getTenantId(),
                        be.getBundleSkuId(),
                        be.getItemSkuId(),
                        code,
                        "Componente " + code,
                        itemSku != null ? itemSku.getPrice() : null,
                        itemSku != null ? itemSku.getNcm() : null,
                        itemSku != null ? itemSku.getCfop() : null,
                        be.getQuantity(),
                        BundleType.valueOf(be.getBundleType()),
                        be.getDiscountPercentage()
                ));
            }
        }

        // Load ebook metadata
        EbookMetadata ebookMetadata = ebookMetadataRepository.findBySkuId(entity.getId())
                .map(em -> new EbookMetadata(
                        em.getId(),
                        em.getTenantId(),
                        em.getSkuId(),
                        em.getFileS3Key(),
                        em.getFileFormat(),
                        em.getFileSizeBytes(),
                        em.isDrmEnabled()
                )).orElse(null);

        // Load images
        List<ProductImage> images = productImageRepository.findAllBySkuIdOrderByDisplayOrderAsc(entity.getId()).stream()
                .map(img -> new ProductImage(
                        img.getId(),
                        img.getTenantId(),
                        img.getProductId(),
                        img.getSkuId(),
                        img.getImageUrl(),
                        img.getS3Key(),
                        img.isPrimary(),
                        img.getDisplayOrder()
                )).toList();

        return new Sku(
                entity.getId(),
                entity.getTenantId(),
                entity.getProductId(),
                entity.getSkuCode(),
                entity.getBarcode(),
                entity.getIsbn(),
                bindingType,
                entity.getEditionYear(),
                entity.getVolume(),
                entity.getPages(),
                entity.getCatalogingSheetText(),
                entity.getWeightGrams(),
                dimensions,
                entity.getPrice(),
                entity.getPromotionalPrice(),
                entity.getCostPrice(),
                entity.getNcm(),
                entity.getCfop(),
                entity.isActive(),
                bundleItems,
                ebookMetadata,
                images,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

