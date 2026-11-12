package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import com.livraria.ecommerce.domain.model.fiscal.FiscalModel;
import com.livraria.ecommerce.domain.model.fiscal.FiscalStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - FiscalInvoice (Entidade de Domínio e Ciclo de Vida Fiscal)")
class FiscalInvoiceTest {

    @Test
    @DisplayName("Deve instanciar NF-e modelo 55 para pedido com valores corretos")
    void shouldCreateForOrder() {
        UUID tenantId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        FiscalInvoice invoice = FiscalInvoice.createForOrder(
                tenantId, companyId, orderId, 1, 105L,
                "35261112345678000190550010000001051123456781",
                new BigDecimal("150.00"), new BigDecimal("10.00"), "VENDA E-COMMERCE"
        );

        assertNotNull(invoice);
        assertEquals(tenantId, invoice.getTenantId());
        assertEquals(companyId, invoice.getCompanyId());
        assertEquals(orderId, invoice.getOrderId());
        assertEquals(FiscalModel.NFE_55, invoice.getModel());
        assertEquals(FiscalStatus.EMITIDA, invoice.getStatus());
        assertEquals(1, invoice.getSeries());
        assertEquals(105L, invoice.getInvoiceNumber());
        assertEquals(new BigDecimal("150.00"), invoice.getTotalAmount());
        assertEquals(new BigDecimal("10.00"), invoice.getTotalDiscount());
    }

    @Test
    @DisplayName("Deve autorizar nota fiscal com protocolo e XML assinado")
    void shouldAuthorizeInvoice() {
        FiscalInvoice invoice = FiscalInvoice.createForOrder(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 100L,
                "key", BigDecimal.TEN, BigDecimal.ZERO, "VENDA"
        );

        invoice.authorize("135260001234567", "<xml>conteudo</xml>");

        assertEquals(FiscalStatus.AUTORIZADA, invoice.getStatus());
        assertEquals("135260001234567", invoice.getProtocolNumber());
        assertEquals("<xml>conteudo</xml>", invoice.getXmlPayload());
        assertNotNull(invoice.getAuthorizedAt());
    }

    @Test
    @DisplayName("Deve cancelar nota fiscal autorizada com justificativa válida")
    void shouldCancelAuthorizedInvoice() {
        FiscalInvoice invoice = FiscalInvoice.createForOrder(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 100L,
                "key", BigDecimal.TEN, BigDecimal.ZERO, "VENDA"
        );
        invoice.authorize("135260001234567", "<xml></xml>");

        String validReason = "Cancelamento por solicitacao expressa do cliente dentro do prazo legal";
        invoice.cancel("135260009999999", validReason);

        assertEquals(FiscalStatus.CANCELADA, invoice.getStatus());
        assertEquals(validReason, invoice.getCancellationReason());
        assertEquals("135260009999999", invoice.getProtocolNumber());
        assertNotNull(invoice.getCanceledAt());
    }

    @Test
    @DisplayName("Deve rejeitar cancelamento com justificativa menor que 15 caracteres")
    void shouldRejectCancelWithShortReason() {
        FiscalInvoice invoice = FiscalInvoice.createForOrder(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 100L,
                "key", BigDecimal.TEN, BigDecimal.ZERO, "VENDA"
        );
        invoice.authorize("135260001234567", "<xml></xml>");

        assertThrows(BusinessException.class, () -> invoice.cancel("prot", "Curto"));
    }

    @Test
    @DisplayName("Deve ativar contingência offline")
    void shouldMarkAsContingency() {
        FiscalInvoice invoice = FiscalInvoice.createForPosSale(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 50L,
                "key", BigDecimal.TEN, BigDecimal.ZERO
        );

        invoice.markAsContingency("<xml>offline</xml>", "Falha SEFAZ");

        assertEquals(FiscalStatus.CONTINGENCIA, invoice.getStatus());
        assertEquals("Falha SEFAZ", invoice.getRejectionReason());
        assertEquals("<xml>offline</xml>", invoice.getXmlPayload());
    }
}

