package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.ecommerce.EcommercePaymentMethod;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderItem;
import com.livraria.ecommerce.domain.model.ecommerce.OrderPayment;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentGateway;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentStatus;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingAddress;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingCarrier;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    private final UUID tenantId = UUID.randomUUID();
    private final ShippingAddress address = new ShippingAddress("Av Paulista", "1000", "Apto 10", "Bela Vista", "São Paulo", "SP", "01310-100");

    @Test
    @DisplayName("Deve criar pedido e calcular valor total com desconto e frete")
    void shouldCreateOrderAndCalculateTotals() {
        OrderItem item1 = new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-01", "Livro Java 21", 2, new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("200.00"), "49019900", "5102");
        OrderItem item2 = new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-02", "Livro Spring Boot", 1, new BigDecimal("80.00"), BigDecimal.ZERO, new BigDecimal("80.00"), "49019900", "5102");

        Order order = Order.create(
                tenantId, "PED-123", UUID.randomUUID(), "João Silva", "joao@email.com",
                "123.456.789-00", "(11) 99999-9999", UUID.randomUUID(),
                new BigDecimal("20.00"), new BigDecimal("15.00"),
                ShippingCarrier.CORREIOS, ShippingService.SEDEX, address,
                List.of(item1, item2), "Deixar na portaria"
        );

        assertThat(order.getSubtotal()).isEqualByComparingTo(new BigDecimal("280.00"));
        assertThat(order.getDiscount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(order.getShippingAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("275.00"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AGUARDANDO_PAGAMENTO);
        assertThat(order.getCustomerDocument()).isEqualTo("12345678900");
    }

    @Test
    @DisplayName("Deve aprovar pagamento e transicionar status para PAGO")
    void shouldMarkAsPaid() {
        OrderItem item = new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-01", "Livro", 1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"), "49019900", "5102");
        Order order = Order.create(tenantId, "PED-123", UUID.randomUUID(), "João Silva", "joao@email.com",
                "12345678900", null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                ShippingCarrier.CORREIOS, ShippingService.PAC, address, List.of(item), null);

        OrderPayment payment = new OrderPayment(UUID.randomUUID(), EcommercePaymentMethod.PIX, PaymentGateway.PAGARME,
                "tx_pix_123", PaymentStatus.PENDENTE, new BigDecimal("50.00"), 1, "qr_code", "copia_cola",
                null, null, null, null, null, Instant.now(), Instant.now(), null);
        order.addPayment(payment);

        order.markAsPaid("tx_pix_123");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAGO);
        assertThat(order.getPaidAt()).isNotNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAGO);
    }

    @Test
    @DisplayName("Deve permitir fluxo logístico completo: Separação -> Envio -> Entrega")
    void shouldHandleLogisticsFlow() {
        OrderItem item = new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-01", "Livro", 1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"), "49019900", "5102");
        Order order = Order.create(tenantId, "PED-123", UUID.randomUUID(), "João", "joao@email.com",
                "12345678900", null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                ShippingCarrier.CORREIOS, ShippingService.SEDEX, address, List.of(item), null);

        order.markAsPaid("tx_1");
        order.startPreparation();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.EM_SEPARACAO);

        order.ship("BR123456789BR", "https://rastreio.correios.com.br?code=BR123456789BR");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENVIADO);
        assertThat(order.getTrackingCode()).isEqualTo("BR123456789BR");
        assertThat(order.getShippedAt()).isNotNull();

        order.deliver();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.ENTREGUE);
        assertThat(order.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve impedir cancelamento de pedido já entregue")
    void shouldPreventCancelingDeliveredOrder() {
        OrderItem item = new OrderItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-01", "Livro", 1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"), "49019900", "5102");
        Order order = Order.create(tenantId, "PED-123", UUID.randomUUID(), "João", "joao@email.com",
                "12345678900", null, null, BigDecimal.ZERO, BigDecimal.ZERO,
                ShippingCarrier.CORREIOS, ShippingService.SEDEX, address, List.of(item), null);
        order.markAsPaid("tx_1");
        order.ship("BR123", null);
        order.deliver();

        assertThatThrownBy(() -> order.cancel("Cliente desistiu"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Não é possível cancelar um pedido já entregue");
    }
}
