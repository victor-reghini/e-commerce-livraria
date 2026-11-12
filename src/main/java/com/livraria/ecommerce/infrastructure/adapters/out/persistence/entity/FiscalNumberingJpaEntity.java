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
@Table(name = "fiscal_numbering_sequences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalNumberingJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "series", nullable = false)
    private int series;

    @Column(name = "current_number", nullable = false)
    private long currentNumber;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

