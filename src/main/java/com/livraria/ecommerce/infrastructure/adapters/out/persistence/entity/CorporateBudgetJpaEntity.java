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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "corporate_budgets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorporateBudgetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "budget_number", nullable = false, length = 50)
    private String budgetNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "corporate_name", nullable = false)
    private String corporateName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(nullable = false, length = 20)
    private String cnpj;

    @Column(name = "state_registration", length = 50)
    private String stateRegistration;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

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

    @Column(name = "payment_terms_days", nullable = false)
    private int paymentTermsDays;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "invoiced_order_id")
    private UUID invoicedOrderId;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CorporateBudgetItemJpaEntity> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;
}

