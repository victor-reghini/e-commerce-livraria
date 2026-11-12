package com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pos_sales")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosSaleJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "shift_id", nullable = false)
    private UUID shiftId;

    @Column(name = "sale_number", nullable = false, length = 64)
    private String saleNumber;

    @Column(name = "client_uuid")
    private UUID clientUuid;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_cpf_cnpj", length = 20)
    private String customerCpfCnpj;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "total_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPaid;

    @Column(name = "change_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal changeAmount;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "nfce_key", length = 64)
    private String nfceKey;

    @Column(name = "nfce_xml", columnDefinition = "TEXT")
    private String nfceXml;

    @Column(name = "nfce_status", nullable = false, length = 32)
    private String nfceStatus;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PosSaleItemJpaEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PosSalePaymentJpaEntity> payments = new ArrayList<>();
}

