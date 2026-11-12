package com.livraria.ecommerce.domain.model.pos;

import java.math.BigDecimal;
import java.util.UUID;

public class PosSalePayment {

    private UUID id;
    private UUID tenantId;
    private UUID saleId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;

    public PosSalePayment(UUID id, UUID tenantId, UUID saleId, PaymentMethod paymentMethod, BigDecimal amount) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.saleId = saleId;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.DINHEIRO;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getSaleId() { return saleId; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public BigDecimal getAmount() { return amount; }
}
