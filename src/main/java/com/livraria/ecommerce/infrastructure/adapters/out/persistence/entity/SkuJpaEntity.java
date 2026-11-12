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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "skus")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_code", nullable = false)
    private String skuCode;

    private String barcode;

    private String isbn;

    @Column(name = "binding_type")
    private String bindingType;

    @Column(name = "edition_year")
    private Integer editionYear;

    private String volume;

    private Integer pages;

    @Column(name = "cataloging_sheet_text", columnDefinition = "TEXT")
    private String catalogingSheetText;

    @Column(name = "weight_grams", nullable = false)
    private int weightGrams;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dimensions_json", columnDefinition = "jsonb")
    private String dimensionsJson;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "promotional_price", precision = 15, scale = 2)
    private BigDecimal promotionalPrice;

    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice;

    private String ncm;

    private String cfop;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

