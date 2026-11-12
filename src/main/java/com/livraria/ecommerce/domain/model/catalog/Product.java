package com.livraria.ecommerce.domain.model.catalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Product {

    private UUID id;
    private UUID tenantId;
    private UUID categoryId;
    private String categoryName;
    private UUID publisherId;
    private String publisherName;
    private String title;
    private String subtitle;
    private String slug;
    private String synopsis;
    private ProductType productType;
    private boolean active;
    private List<ProductAuthor> authors;
    private List<Sku> skus;
    private Instant createdAt;
    private Instant updatedAt;

    public Product(UUID id, UUID tenantId, UUID categoryId, String categoryName, UUID publisherId,
                   String publisherName, String title, String subtitle, String slug, String synopsis,
                   ProductType productType, boolean active, List<ProductAuthor> authors,
                   List<Sku> skus, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.publisherId = publisherId;
        this.publisherName = publisherName;
        this.title = title;
        this.subtitle = subtitle;
        this.slug = slug;
        this.synopsis = synopsis;
        this.productType = productType != null ? productType : ProductType.BOOK;
        this.active = active;
        this.authors = authors != null ? new ArrayList<>(authors) : new ArrayList<>();
        this.skus = skus != null ? new ArrayList<>(skus) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();

        validate();
    }

    public static Product create(UUID tenantId, UUID categoryId, UUID publisherId, String title,
                                 String subtitle, String slug, String synopsis, ProductType productType) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O título do produto é obrigatório.");
        }
        if (slug == null || slug.isBlank()) {
            slug = generateSlug(title);
        }
        return new Product(
                UUID.randomUUID(), tenantId, categoryId, null, publisherId, null,
                title.trim(), subtitle, slug.trim(), synopsis,
                productType != null ? productType : ProductType.BOOK,
                true, new ArrayList<>(), new ArrayList<>(),
                Instant.now(), Instant.now()
        );
    }

    private void validate() {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O título do produto é obrigatório.");
        }
        if (slug == null || slug.isBlank()) {
            this.slug = generateSlug(title);
        }
    }

    public static String generateSlug(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("[áàãâä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòõôö]", "o")
                .replaceAll("[úùûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    public void update(String title, String subtitle, String slug, String synopsis,
                       UUID categoryId, UUID publisherId, ProductType productType, boolean active) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O título do produto é obrigatório.");
        }
        this.title = title.trim();
        this.subtitle = subtitle;
        this.slug = (slug != null && !slug.isBlank()) ? slug.trim() : generateSlug(title);
        this.synopsis = synopsis;
        this.categoryId = categoryId;
        this.publisherId = publisherId;
        this.productType = productType != null ? productType : this.productType;
        this.active = active;
        this.updatedAt = Instant.now();
    }

    public void addAuthor(UUID authorId, String authorName, AuthorRole role, int displayOrder) {
        this.authors.removeIf(a -> a.getAuthorId().equals(authorId) && a.getRole() == role);
        this.authors.add(new ProductAuthor(this.id, authorId, authorName, role, displayOrder));
        this.updatedAt = Instant.now();
    }

    public void setAuthors(List<ProductAuthor> authors) {
        this.authors = authors != null ? new ArrayList<>(authors) : new ArrayList<>();
        this.updatedAt = Instant.now();
    }

    public void addSku(Sku sku) {
        if (sku != null) {
            this.skus.removeIf(s -> s.getId().equals(sku.getId()) || s.getSkuCode().equalsIgnoreCase(sku.getSkuCode()));
            this.skus.add(sku);
            this.updatedAt = Instant.now();
        }
    }

    public void removeSku(UUID skuId) {
        if (skuId != null) {
            this.skus.removeIf(s -> s.getId().equals(skuId));
            this.updatedAt = Instant.now();
        }
    }

    public Sku getPrimarySku() {
        return skus.stream().filter(Sku::isActive).findFirst().orElse(null);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public UUID getPublisherId() { return publisherId; }
    public String getPublisherName() { return publisherName; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getSlug() { return slug; }
    public String getSynopsis() { return synopsis; }
    public ProductType getProductType() { return productType; }
    public boolean isActive() { return active; }
    public List<ProductAuthor> getAuthors() { return Collections.unmodifiableList(authors); }
    public List<Sku> getSkus() { return Collections.unmodifiableList(skus); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}

