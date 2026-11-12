package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.pos.CashMovementType;
import com.livraria.ecommerce.domain.model.pos.CashShift;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.ShiftStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - CashShift (Domínio de Turno de Caixa, Suprimento, Sangria e Fechamento Cego)")
class CashShiftTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID cashierId = UUID.randomUUID();

    @Test
    void shouldOpenShiftWithInitialBalance() {
        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "João Caixa",
                "CX-001", ShiftStatus.ABERTO, new BigDecimal("100.00"), null, null, null,
                null, new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        assertEquals(ShiftStatus.ABERTO, shift.getStatus());
        assertEquals(new BigDecimal("100.00"), shift.getOpeningBalance());
        assertEquals(new BigDecimal("100.00"), shift.calculateExpectedCashBalance());
        assertNull(shift.getClosedAt());
    }

    @Test
    void shouldAddSupplySuccessfully() {
        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "João Caixa",
                "CX-002", ShiftStatus.ABERTO, new BigDecimal("50.00"), null, null, null,
                null, new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        shift.addSupply(new BigDecimal("100.00"), "Troco adicional", cashierId);

        assertEquals(1, shift.getMovements().size());
        assertEquals(CashMovementType.SUPRIMENTO, shift.getMovements().get(0).getMovementType());
        // Saldo esperado em dinheiro = R$ 50,00 + R$ 100,00 = R$ 150,00
        assertEquals(new BigDecimal("150.00"), shift.calculateExpectedCashBalance());
    }

    @Test
    void shouldAddBleedSuccessfully() {
        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "João Caixa",
                "CX-003", ShiftStatus.ABERTO, new BigDecimal("200.00"), null, null, null,
                null, new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        shift.addBleed(new BigDecimal("150.00"), "Sangria para cofre", cashierId);

        assertEquals(1, shift.getMovements().size());
        assertEquals(CashMovementType.SANGRIA, shift.getMovements().get(0).getMovementType());
        // Saldo esperado em dinheiro = R$ 200,00 - R$ 150,00 = R$ 50,00
        assertEquals(new BigDecimal("50.00"), shift.calculateExpectedCashBalance());
    }

    @Test
    void shouldRejectBleedWhenAmountExceedsCurrentCashInDrawer() {
        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "João Caixa",
                "CX-004", ShiftStatus.ABERTO, new BigDecimal("50.00"), null, null, null,
                null, new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        // Tentativa de sangria de R$ 100,00 com apenas R$ 50,00 em gaveta
        assertThrows(BusinessException.class, () ->
                shift.addBleed(new BigDecimal("100.00"), "Sangria excessiva", cashierId)
        );
    }

    @Test
    void shouldRecordSalesAndCalculateBalancesByPaymentMethod() {
        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "João Caixa",
                "CX-005", ShiftStatus.ABERTO, new BigDecimal("100.00"), null, null, null,
                null, new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        // Venda 1: R$ 80,00 em dinheiro
        shift.recordSalePayment(new BigDecimal("80.00"), PaymentMethod.DINHEIRO, cashierId);
        // Venda 2: R$ 120,00 no PIX
        shift.recordSalePayment(new BigDecimal("120.00"), PaymentMethod.PIX, cashierId);
        // Venda 3: R$ 200,00 no Cartão de Crédito
        shift.recordSalePayment(new BigDecimal("200.00"), PaymentMethod.CARTAO_CREDITO, cashierId);

        assertEquals(new BigDecimal("180.00"), shift.calculateExpectedCashBalance()); // R$ 100 + R$ 80
        assertEquals(new BigDecimal("80.00"), shift.calculateTotalByMethod(PaymentMethod.DINHEIRO));
        assertEquals(new BigDecimal("120.00"), shift.calculateTotalByMethod(PaymentMethod.PIX));
        assertEquals(new BigDecimal("200.00"), shift.calculateTotalByMethod(PaymentMethod.CARTAO_CREDITO));
        assertEquals(BigDecimal.ZERO, shift.calculateTotalByMethod(PaymentMethod.CARTAO_DEBITO));
    }

    @Test
    void shouldCloseBlindShiftAndComputeDivergence() {
        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "João Caixa",
                "CX-006", ShiftStatus.ABERTO, new BigDecimal("100.00"), null, null, null,
                null, new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        // Vendas em dinheiro: + R$ 250,00 => Esperado = R$ 350,00
        shift.recordSalePayment(new BigDecimal("250.00"), PaymentMethod.DINHEIRO, cashierId);

        // Fechamento cego: operador contou R$ 348,00 (falta de R$ 2,00 / quebra negativa)
        shift.closeBlind(new BigDecimal("348.00"), "Fechamento noturno");

        assertEquals(ShiftStatus.FECHADO, shift.getStatus());
        assertNotNull(shift.getClosedAt());
        assertEquals(new BigDecimal("350.00"), shift.getExpectedBalance());
        assertEquals(new BigDecimal("348.00"), shift.getActualBalance());
        assertEquals(new BigDecimal("-2.00"), shift.getDivergence());
    }

    @Test
    void shouldRejectOperationsWhenShiftIsClosed() {
        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "João Caixa",
                "CX-007", ShiftStatus.FECHADO, new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO, null, new ArrayList<>(),
                Instant.now(), Instant.now(), Instant.now(), Instant.now()
        );

        assertThrows(BusinessException.class, () -> shift.addSupply(BigDecimal.TEN, "troco", cashierId));
        assertThrows(BusinessException.class, () -> shift.addBleed(BigDecimal.TEN, "sangria", cashierId));
        assertThrows(BusinessException.class, () -> shift.closeBlind(BigDecimal.TEN, "fechar"));
    }
}
