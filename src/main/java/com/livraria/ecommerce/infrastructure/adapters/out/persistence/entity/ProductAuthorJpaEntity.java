package com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "product_authors")
@IdClass(ProductAuthorId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAuthorJpaEntity {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Id
    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Id
    @Column(nullable = false)
    private String role;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}

