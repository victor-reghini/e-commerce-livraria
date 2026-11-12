package com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "blind_audit_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlindAuditItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_id", nullable = false)
    private BlindAuditJpaEntity audit;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "quantity_scanned", nullable = false)
    private int quantityScanned;

    @Column(name = "quantity_expected", nullable = false)
    private int quantityExpected;

    @Column(name = "divergence", nullable = false)
    private int divergence;

    @Column(name = "last_scanned_at")
    private Instant lastScannedAt;
}

