package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.pos.NfceStatus;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.PosSalePayment;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - PosSale (Domínio de Venda no Balcão, Descontos, Troco e Cancelamento)")
class PosSaleTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID shiftId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldCreateSaleAndCalculateTrocoAccurately() {
        UUID skuId = UUID.randomUUID();
        // 2 livros de R$ 40,00 = Subtotal R$ 80,00
        PosSaleItem item = new PosSaleItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId, "LIV-01", "Livro Teste",
                2, new BigDecimal("40.00"), BigDecimal.ZERO, new BigDecimal("80.00")
        );

        // Cliente dá uma nota de R$ 100,00 em dinheiro
        PosSalePayment payment = new PosSalePayment(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), PaymentMethod.DINHEIRO, new BigDecimal("100.00")
        );

        PosSale sale = new PosSale(
                UUID.randomUUID(), tenantId, companyId, shiftId, "VD-001", null,
                "Consumidor Final", "123.456.789-00", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, SaleStatus.CONCLUIDA,
                "35261112345678000195650010000000011123456789", null, NfceStatus.EMITIDA,
                List.of(item), List.of(payment), userId, Instant.now(), Instant.now()
        );

        assertEquals(new BigDecimal("80.00"), sale.getSubtotal());
        assertEquals(new BigDecimal("80.00"), sale.getTotal());
        assertEquals(new BigDecimal("100.00"), sale.getTotalPaid());
        assertEquals(new BigDecimal("20.00"), sale.getChangeAmount()); // Troco de R$ 20,00
        assertEquals(SaleStatus.CONCLUIDA, sale.getStatus());
        assertEquals(NfceStatus.EMITIDA, sale.getNfceStatus());
    }

    @Test
    void shouldSupportSplitPaymentsAcrossDifferentMethods() {
        UUID skuId1 = UUID.randomUUID();
        UUID skuId2 = UUID.randomUUID();

        PosSaleItem item1 = new PosSaleItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId1, "LIV-A", "Livro A",
                1, new BigDecimal("70.00"), BigDecimal.ZERO, new BigDecimal("70.00")
        );
        PosSaleItem item2 = new PosSaleItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId2, "LIV-B", "Livro B",
                1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00")
        );

        // Subtotal = R$ 120,00 com desconto de R$ 20,00 => Total = R$ 100,00
        // Pago: R$ 60,00 no PIX + R$ 40,00 no Cartão de Débito
        PosSalePayment p1 = new PosSalePayment(UUID.randomUUID(), tenantId, UUID.randomUUID(), PaymentMethod.PIX, new BigDecimal("60.00"));
        PosSalePayment p2 = new PosSalePayment(UUID.randomUUID(), tenantId, UUID.randomUUID(), PaymentMethod.CARTAO_DEBITO, new BigDecimal("40.00"));

        PosSale sale = new PosSale(
                UUID.randomUUID(), tenantId, companyId, shiftId, "VD-002", null,
                null, null, BigDecimal.ZERO, new BigDecimal("20.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, SaleStatus.CONCLUIDA,
                null, null, NfceStatus.EMITIDA,
                List.of(item1, item2), List.of(p1, p2), userId, Instant.now(), Instant.now()
        );

        assertEquals(new BigDecimal("120.00"), sale.getSubtotal());
        assertEquals(new BigDecimal("20.00"), sale.getDiscount());
        assertEquals(new BigDecimal("100.00"), sale.getTotal());
        assertEquals(new BigDecimal("100.00"), sale.getTotalPaid());
        assertEquals(BigDecimal.ZERO, sale.getChangeAmount());
    }

    @Test
    void shouldRejectSaleWithEmptyItems() {
        PosSalePayment p = new PosSalePayment(UUID.randomUUID(), tenantId, UUID.randomUUID(), PaymentMethod.PIX, BigDecimal.TEN);
        assertThrows(BusinessException.class, () -> new PosSale(
                UUID.randomUUID(), tenantId, companyId, shiftId, "VD-ERR", null,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, SaleStatus.CONCLUIDA,
                null, null, NfceStatus.EMITIDA,
                Collections.emptyList(), List.of(p), userId, Instant.now(), Instant.now()
        ));
    }

    @Test
    void shouldRejectSaleWhenTotalPaidIsInsufficient() {
        PosSaleItem item = new PosSaleItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(), "LIV-01", "Livro",
                1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00")
        );
        // Cliente paga apenas R$ 30,00 para compra de R$ 50,00
        PosSalePayment p = new PosSalePayment(UUID.randomUUID(), tenantId, UUID.randomUUID(), PaymentMethod.DINHEIRO, new BigDecimal("30.00"));

        assertThrows(BusinessException.class, () -> new PosSale(
                UUID.randomUUID(), tenantId, companyId, shiftId, "VD-ERR2", null,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, SaleStatus.CONCLUIDA,
                null, null, NfceStatus.EMITIDA,
                List.of(item), List.of(p), userId, Instant.now(), Instant.now()
        ));
    }

    @Test
    void shouldCancelSaleSuccessfully() {
        PosSaleItem item = new PosSaleItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), UUID.randomUUID(), "LIV-01", "Livro",
                1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00")
        );
        PosSalePayment p = new PosSalePayment(UUID.randomUUID(), tenantId, UUID.randomUUID(), PaymentMethod.DINHEIRO, new BigDecimal("50.00"));

        PosSale sale = new PosSale(
                UUID.randomUUID(), tenantId, companyId, shiftId, "VD-003", null,
                null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, SaleStatus.CONCLUIDA,
                "KEY44", "XML", NfceStatus.EMITIDA,
                List.of(item), List.of(p), userId, Instant.now(), Instant.now()
        );

        sale.cancel();

        assertEquals(SaleStatus.CANCELADA, sale.getStatus());
        assertEquals(NfceStatus.CANCELADA, sale.getNfceStatus());

        assertThrows(BusinessException.class, sale::cancel);
    }
}

