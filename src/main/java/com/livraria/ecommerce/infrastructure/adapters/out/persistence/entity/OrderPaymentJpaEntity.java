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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "order_payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderJpaEntity order;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(nullable = false, length = 50)
    private String gateway;

    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private int installments;

    @Column(name = "pix_qr_code", columnDefinition = "TEXT")
    private String pixQrCode;

    @Column(name = "pix_copia_cola", columnDefinition = "TEXT")
    private String pixCopiaCola;

    @Column(name = "boleto_barcode", length = 100)
    private String boletoBarcode;

    @Column(name = "boleto_url", length = 500)
    private String boletoUrl;

    @Column(name = "boleto_due_date")
    private LocalDate boletoDueDate;

    @Column(name = "card_brand", length = 50)
    private String cardBrand;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "paid_at")
    private Instant paidAt;
}
