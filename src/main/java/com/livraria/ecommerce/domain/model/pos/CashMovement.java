package com.livraria.ecommerce.domain.model.pos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CashMovement {

    private UUID id;
    private UUID tenantId;
    private UUID shiftId;
    private CashMovementType movementType;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String reason;
    private UUID operatorId;
    private Instant createdAt;

    public CashMovement(UUID id, UUID tenantId, UUID shiftId, CashMovementType movementType,
                        BigDecimal amount, PaymentMethod paymentMethod, String reason,
                        UUID operatorId, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.shiftId = shiftId;
        this.movementType = movementType;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.DINHEIRO;
        this.reason = reason;
        this.operatorId = operatorId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getShiftId() { return shiftId; }
    public CashMovementType getMovementType() { return movementType; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getReason() { return reason; }
    public UUID getOperatorId() { return operatorId; }
    public Instant getCreatedAt() { return createdAt; }
}

