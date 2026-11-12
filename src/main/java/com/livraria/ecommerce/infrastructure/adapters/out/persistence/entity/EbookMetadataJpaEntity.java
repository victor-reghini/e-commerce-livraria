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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ebook_metadata")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EbookMetadataJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "sku_id", nullable = false, unique = true)
    private UUID skuId;

    @Column(name = "file_s3_key", nullable = false)
    private String fileS3Key;

    @Column(name = "file_format", nullable = false)
    private String fileFormat;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "drm_enabled", nullable = false)
    private boolean drmEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

