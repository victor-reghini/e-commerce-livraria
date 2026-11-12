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
@Table(name = "fiscal_invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalInvoiceJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "series", nullable = false)
    private int series;

    @Column(name = "invoice_number", nullable = false)
    private long invoiceNumber;

    @Column(name = "access_key", nullable = false, length = 44, unique = true)
    private String accessKey;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "nature_of_operation", nullable = false, length = 100)
    private String natureOfOperation;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "total_discount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalDiscount;

    @Column(name = "total_tax_icms", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTaxIcms;

    @Column(name = "total_tax_pis", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTaxPis;

    @Column(name = "total_tax_cofins", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalTaxCofins;

    @Column(name = "protocol_number", length = 100)
    private String protocolNumber;

    @Column(name = "xml_payload", columnDefinition = "TEXT")
    private String xmlPayload;

    @Column(name = "danfe_url", length = 500)
    private String danfeUrl;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;
}
