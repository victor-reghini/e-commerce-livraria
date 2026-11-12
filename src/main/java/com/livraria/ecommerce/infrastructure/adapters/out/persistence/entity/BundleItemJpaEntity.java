package com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bundle_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BundleItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "bundle_sku_id", nullable = false)
    private UUID bundleSkuId;

    @Column(name = "item_sku_id", nullable = false)
    private UUID itemSkuId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "bundle_type", nullable = false)
    private String bundleType;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

