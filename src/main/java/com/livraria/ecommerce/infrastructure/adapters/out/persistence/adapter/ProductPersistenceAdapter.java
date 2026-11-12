package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.catalog.Author;
import com.livraria.ecommerce.domain.model.catalog.AuthorRole;
import com.livraria.ecommerce.domain.model.catalog.Category;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.ProductAuthor;
import com.livraria.ecommerce.domain.model.catalog.ProductType;
import com.livraria.ecommerce.domain.model.catalog.Publisher;
import com.livraria.ecommerce.domain.model.catalog.Sku;
import com.livraria.ecommerce.domain.port.out.AuthorRepositoryPort;
import com.livraria.ecommerce.domain.port.out.CategoryRepositoryPort;
import com.livraria.ecommerce.domain.port.out.ProductRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PublisherRepositoryPort;
import com.livraria.ecommerce.domain.port.out.SkuRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductAuthorJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductAuthorRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductPersistenceAdapter implements ProductRepositoryPort {

    private final SpringDataProductRepository productRepository;
    private final SpringDataProductAuthorRepository productAuthorRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final PublisherRepositoryPort publisherRepository;
    private final AuthorRepositoryPort authorRepository;
    private final SkuRepositoryPort skuRepository;

    public ProductPersistenceAdapter(SpringDataProductRepository productRepository,
                                     SpringDataProductAuthorRepository productAuthorRepository,
                                     CategoryRepositoryPort categoryRepository,
                                     PublisherRepositoryPort publisherRepository,
                                     AuthorRepositoryPort authorRepository,
                                     SkuRepositoryPort skuRepository) {
        this.productRepository = productRepository;
        this.productAuthorRepository = productAuthorRepository;
        this.categoryRepository = categoryRepository;
        this.publisherRepository = publisherRepository;
        this.authorRepository = authorRepository;
        this.skuRepository = skuRepository;
    }

    @Override
    @Transactional
    public Product save(Product product) {
        ProductJpaEntity entity = ProductJpaEntity.builder()
                .id(product.getId())
                .tenantId(product.getTenantId())
                .categoryId(product.getCategoryId())
                .publisherId(product.getPublisherId())
                .title(product.getTitle())
                .subtitle(product.getSubtitle())
                .slug(product.getSlug())
                .synopsis(product.getSynopsis())
                .productType(product.getProductType() != null ? product.getProductType().name() : ProductType.BOOK.name())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();

        ProductJpaEntity saved = productRepository.save(entity);

        // Authors
        productAuthorRepository.deleteAllByProductId(saved.getId());
        if (product.getAuthors() != null && !product.getAuthors().isEmpty()) {
            List<ProductAuthorJpaEntity> authorEntities = product.getAuthors().stream()
                    .map(pa -> ProductAuthorJpaEntity.builder()
                            .productId(saved.getId())
                            .authorId(pa.getAuthorId())
                            .role(pa.getRole().name())
                            .tenantId(saved.getTenantId())
                            .displayOrder(pa.getDisplayOrder())
                            .build())
                    .toList();
            productAuthorRepository.saveAll(authorEntities);
        }

        // Skus
        if (product.getSkus() != null) {
            for (Sku sku : product.getSkus()) {
                skuRepository.save(sku);
            }
        }

        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(UUID id) {
        return productRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findBySlug(UUID tenantId, String slug) {
        return productRepository.findByTenantIdAndSlug(tenantId, slug).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> search(UUID tenantId, String query, UUID categoryId, UUID publisherId, Boolean active, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ProductJpaEntity> result = productRepository.search(tenantId, query, categoryId, publisherId, active, pageRequest);
        return result.getContent().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countSearch(UUID tenantId, String query, UUID categoryId, UUID publisherId, Boolean active) {
        PageRequest pageRequest = PageRequest.of(0, 1);
        Page<ProductJpaEntity> result = productRepository.search(tenantId, query, categoryId, publisherId, active, pageRequest);
        return result.getTotalElements();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        List<Sku> skus = skuRepository.findAllByProductId(id);
        for (Sku sku : skus) {
            skuRepository.deleteById(sku.getId());
        }
        productAuthorRepository.deleteAllByProductId(id);
        productRepository.deleteById(id);
    }

    private Product toDomain(ProductJpaEntity entity) {
        String categoryName = null;
        if (entity.getCategoryId() != null) {
            categoryName = categoryRepository.findById(entity.getCategoryId())
                    .map(Category::getName)
                    .orElse(null);
        }

        String publisherName = null;
        if (entity.getPublisherId() != null) {
            publisherName = publisherRepository.findById(entity.getPublisherId())
                    .map(Publisher::getName)
                    .orElse(null);
        }

        // Load authors
        List<ProductAuthorJpaEntity> paEntities = productAuthorRepository.findAllByProductIdOrderByDisplayOrderAsc(entity.getId());
        List<ProductAuthor> authors = new ArrayList<>();
        if (!paEntities.isEmpty()) {
            List<UUID> authorIds = paEntities.stream().map(ProductAuthorJpaEntity::getAuthorId).toList();
            Map<UUID, String> authorNames = authorRepository.findAllByTenantId(entity.getTenantId()).stream()
                    .filter(a -> authorIds.contains(a.getId()))
                    .collect(Collectors.toMap(Author::getId, Author::getName));

            for (ProductAuthorJpaEntity pae : paEntities) {
                authors.add(new ProductAuthor(
                        pae.getProductId(),
                        pae.getAuthorId(),
                        authorNames.getOrDefault(pae.getAuthorId(), "Autor Desconhecido"),
                        AuthorRole.valueOf(pae.getRole()),
                        pae.getDisplayOrder()
                ));
            }
        }

        // Load skus
        List<Sku> skus = skuRepository.findAllByProductId(entity.getId());

        ProductType pType = ProductType.BOOK;
        if (entity.getProductType() != null) {
            try {
                pType = ProductType.valueOf(entity.getProductType());
            } catch (IllegalArgumentException ignored) {}
        }

        return new Product(
                entity.getId(),
                entity.getTenantId(),
                entity.getCategoryId(),
                categoryName,
                entity.getPublisherId(),
                publisherName,
                entity.getTitle(),
                entity.getSubtitle(),
                entity.getSlug(),
                entity.getSynopsis(),
                pType,
                entity.isActive(),
                authors,
                skus,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

