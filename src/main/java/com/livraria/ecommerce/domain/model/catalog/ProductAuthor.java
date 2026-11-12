package com.livraria.ecommerce.domain.model.catalog;

import java.util.UUID;

public class ProductAuthor {

    private UUID productId;
    private UUID authorId;
    private String authorName;
    private AuthorRole role;
    private int displayOrder;

    public ProductAuthor(UUID productId, UUID authorId, String authorName, AuthorRole role, int displayOrder) {
        this.productId = productId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.role = role != null ? role : AuthorRole.AUTHOR;
        this.displayOrder = displayOrder;
    }

    public UUID getProductId() { return productId; }
    public UUID getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public AuthorRole getRole() { return role; }
    public int getDisplayOrder() { return displayOrder; }
}

