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
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "customer_document", nullable = false, length = 50)
    private String customerDocument;

    @Column(name = "customer_phone", length = 50)
    private String customerPhone;

    @Column(name = "origin_company_id")
    private UUID originCompanyId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal discount;

    @Column(name = "shipping_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingAmount;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_carrier", length = 100)
    private String shippingCarrier;

    @Column(name = "shipping_service", length = 100)
    private String shippingService;

    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @Column(name = "shipping_address_street")
    private String shippingAddressStreet;

    @Column(name = "shipping_address_number", length = 50)
    private String shippingAddressNumber;

    @Column(name = "shipping_address_complement")
    private String shippingAddressComplement;

    @Column(name = "shipping_address_neighborhood", length = 100)
    private String shippingAddressNeighborhood;

    @Column(name = "shipping_address_city", length = 100)
    private String shippingAddressCity;

    @Column(name = "shipping_address_state", length = 2)
    private String shippingAddressState;

    @Column(name = "shipping_address_zip_code", length = 20)
    private String shippingAddressZipCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderPaymentJpaEntity> payments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;
}

