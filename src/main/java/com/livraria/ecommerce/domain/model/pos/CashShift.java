package com.livraria.ecommerce.domain.model.pos;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CashShift {

    private UUID id;
    private UUID tenantId;
    private UUID companyId;
    private UUID cashierId;
    private String cashierName;
    private String shiftNumber;
    private ShiftStatus status;
    private BigDecimal openingBalance;
    private BigDecimal expectedBalance;
    private BigDecimal actualBalance;
    private BigDecimal divergence;
    private String notes;
    private List<CashMovement> movements;
    private Instant openedAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public CashShift(UUID id, UUID tenantId, UUID companyId, UUID cashierId, String cashierName,
                     String shiftNumber, ShiftStatus status, BigDecimal openingBalance,
                     BigDecimal expectedBalance, BigDecimal actualBalance, BigDecimal divergence,
                     String notes, List<CashMovement> movements, Instant openedAt, Instant closedAt,
                     Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.cashierId = cashierId;
        this.cashierName = cashierName;
        this.shiftNumber = shiftNumber;
        this.status = status != null ? status : ShiftStatus.ABERTO;
        this.openingBalance = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        this.expectedBalance = expectedBalance;
        this.actualBalance = actualBalance;
        this.divergence = divergence;
        this.notes = notes;
        this.movements = movements != null ? new ArrayList<>(movements) : new ArrayList<>();
        this.openedAt = openedAt != null ? openedAt : Instant.now();
        this.closedAt = closedAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public void addSupply(BigDecimal amount, String reason, UUID operatorId) {
        ensureOpen();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor de suprimento deve ser maior que zero.");
        }
        this.movements.add(new CashMovement(
                UUID.randomUUID(), tenantId, id, CashMovementType.SUPRIMENTO,
                amount, PaymentMethod.DINHEIRO, reason != null ? reason : "Suprimento de troco", operatorId, Instant.now()
        ));
        this.updatedAt = Instant.now();
    }

    public void addBleed(BigDecimal amount, String reason, UUID operatorId) {
        ensureOpen();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor da sangria deve ser maior que zero.");
        }
        BigDecimal currentCash = calculateExpectedCashBalance();
        if (currentCash.compareTo(amount) < 0) {
            throw new BusinessException("Saldo em dinheiro insuficiente no caixa para efetuar a sangria. Disponível: R$ "
                    + currentCash + ", Solicitado: R$ " + amount);
        }
        this.movements.add(new CashMovement(
                UUID.randomUUID(), tenantId, id, CashMovementType.SANGRIA,
                amount, PaymentMethod.DINHEIRO, reason != null ? reason : "Sangria para cofre", operatorId, Instant.now()
        ));
        this.updatedAt = Instant.now();
    }

    public void recordSalePayment(BigDecimal amount, PaymentMethod method, UUID operatorId) {
        ensureOpen();
        this.movements.add(new CashMovement(
                UUID.randomUUID(), tenantId, id, CashMovementType.VENDA,
                amount, method, "Recebimento de venda", operatorId, Instant.now()
        ));
        this.updatedAt = Instant.now();
    }

    public void closeBlind(BigDecimal countedCash, String closeNotes) {
        ensureOpen();
        if (countedCash == null || countedCash.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O valor em dinheiro contado não pode ser negativo.");
        }
        this.expectedBalance = calculateExpectedCashBalance();
        this.actualBalance = countedCash;
        this.divergence = countedCash.subtract(this.expectedBalance);
        this.status = ShiftStatus.FECHADO;
        this.notes = closeNotes;
        this.closedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public BigDecimal calculateExpectedCashBalance() {
        BigDecimal balance = this.openingBalance;
        for (CashMovement m : movements) {
            if (m.getPaymentMethod() == PaymentMethod.DINHEIRO) {
                if (m.getMovementType() == CashMovementType.SUPRIMENTO || m.getMovementType() == CashMovementType.VENDA) {
                    balance = balance.add(m.getAmount());
                } else if (m.getMovementType() == CashMovementType.SANGRIA || m.getMovementType() == CashMovementType.ESTORNO) {
                    balance = balance.subtract(m.getAmount());
                }
            }
        }
        return balance;
    }

    public BigDecimal calculateTotalByMethod(PaymentMethod method) {
        BigDecimal total = BigDecimal.ZERO;
        for (CashMovement m : movements) {
            if (m.getPaymentMethod() == method) {
                if (m.getMovementType() == CashMovementType.VENDA) {
                    total = total.add(m.getAmount());
                } else if (m.getMovementType() == CashMovementType.ESTORNO) {
                    total = total.subtract(m.getAmount());
                }
            }
        }
        return total;
    }

    private void ensureOpen() {
        if (this.status != ShiftStatus.ABERTO) {
            throw new BusinessException("Operação não permitida. O turno de caixa está FECHADO.");
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCompanyId() { return companyId; }
    public UUID getCashierId() { return cashierId; }
    public String getCashierName() { return cashierName; }
    public String getShiftNumber() { return shiftNumber; }
    public ShiftStatus getStatus() { return status; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public BigDecimal getExpectedBalance() { return expectedBalance; }
    public BigDecimal getActualBalance() { return actualBalance; }
    public BigDecimal getDivergence() { return divergence; }
    public String getNotes() { return notes; }
    public List<CashMovement> getMovements() { return Collections.unmodifiableList(movements); }
    public Instant getOpenedAt() { return openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
