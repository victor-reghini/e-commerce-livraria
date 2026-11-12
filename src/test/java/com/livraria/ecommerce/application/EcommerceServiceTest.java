package com.livraria.ecommerce.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.application.dto.ecommerce.EcommerceDto;
import com.livraria.ecommerce.application.service.EcommerceService;
import com.livraria.ecommerce.domain.model.ecommerce.Cart;
import com.livraria.ecommerce.domain.model.ecommerce.EcommercePaymentMethod;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentStatus;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentTransactionResult;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingCarrier;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingQuote;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingService;
import com.livraria.ecommerce.domain.port.out.CartRepositoryPort;
import com.livraria.ecommerce.domain.port.out.OrderRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PagarmeGatewayPort;
import com.livraria.ecommerce.domain.port.out.ShippingCalculatorPort;
import com.livraria.ecommerce.domain.port.out.WebhookEventRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EcommerceServiceTest {

    @Mock private CartRepositoryPort cartRepository;
    @Mock private OrderRepositoryPort orderRepository;
    @Mock private WebhookEventRepositoryPort webhookRepository;
    @Mock private PagarmeGatewayPort pagarmeGateway;
    @Mock private ShippingCalculatorPort shippingCalculator;
    @Mock private SpringDataSkuRepository skuRepository;
    @Mock private SpringDataProductRepository productRepository;
    @Mock private SpringDataStockItemRepository stockItemRepository;
    @Mock private SpringDataStockMovementRepository stockMovementRepository;
    @Mock private SpringDataCompanyRepository companyRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private EcommerceService ecommerceService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID skuId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        ecommerceService = new EcommerceService(
                cartRepository, orderRepository, webhookRepository, pagarmeGateway,
                shippingCalculator, skuRepository, productRepository, stockItemRepository,
                stockMovementRepository, companyRepository, rabbitTemplate, objectMapper
        );
    }

    @Test
    @DisplayName("Deve adicionar item ao carrinho de compras")
    void shouldAddItemToCart() {
        Cart cart = Cart.createNew(tenantId, "sess_1", null);
        when(cartRepository.findByTenantIdAndSessionId(tenantId, "sess_1")).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).productId(productId).skuCode("SKU-1").price(new BigDecimal("99.90")).build();
        ProductJpaEntity prod = ProductJpaEntity.builder().id(productId).title("Clean Architecture").build();
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(productRepository.findById(productId)).thenReturn(Optional.of(prod));

        Cart updated = ecommerceService.addItemToCart(tenantId, "sess_1", null, skuId, 2);

        assertThat(updated.calculateTotalItems()).isEqualTo(2);
        assertThat(updated.calculateSubtotal()).isEqualByComparingTo(new BigDecimal("199.80"));
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Deve calcular cotações de frete para carrinho com itens")
    void shouldCalculateShipping() {
        Cart cart = Cart.createNew(tenantId, "sess_1", null);
        cart.addItem(skuId, "SKU-1", "Livro", 1, new BigDecimal("50.00"));
        when(cartRepository.findByTenantIdAndSessionId(tenantId, "sess_1")).thenReturn(Optional.of(cart));

        CompanyJpaEntity matriz = CompanyJpaEntity.builder().id(companyId).name("Matriz").headquarters(true).build();
        when(companyRepository.findAllByTenantId(tenantId)).thenReturn(List.of(matriz));

        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).weightGrams(500).build();
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));

        List<ShippingQuote> quotes = List.of(
                ShippingQuote.available(ShippingCarrier.CORREIOS, ShippingService.SEDEX, "SEDEX", new BigDecimal("25.00"), 2),
                ShippingQuote.available(ShippingCarrier.CORREIOS, ShippingService.PAC, "PAC", new BigDecimal("15.00"), 6)
        );
        when(shippingCalculator.calculate(any(), any(), any(), anyInt(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(quotes);

        List<ShippingQuote> result = ecommerceService.calculateShipping(tenantId, "01310-100", "sess_1", null);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Deve realizar Checkout com PIX gerando reserva de estoque e status AGUARDANDO_PAGAMENTO")
    void shouldCheckoutWithPix() {
        Cart cart = Cart.createNew(tenantId, "sess_1", null);
        cart.addItem(skuId, "SKU-1", "Livro", 1, new BigDecimal("80.00"));
        when(cartRepository.findByTenantIdAndSessionId(tenantId, "sess_1")).thenReturn(Optional.of(cart));

        CompanyJpaEntity matriz = CompanyJpaEntity.builder().id(companyId).name("Matriz").headquarters(true).build();
        when(companyRepository.findAllByTenantId(tenantId)).thenReturn(List.of(matriz));

        StockItemJpaEntity stock = StockItemJpaEntity.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .companyId(companyId).skuId(skuId).quantity(10).reservedQuantity(0).build();
        when(stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId)).thenReturn(Optional.of(stock));

        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).skuCode("SKU-1").price(new BigDecimal("80.00")).weightGrams(400).build();
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));

        when(shippingCalculator.calculate(any(), any(), any(), anyInt(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(ShippingQuote.available(ShippingCarrier.CORREIOS, ShippingService.SEDEX, "SEDEX", new BigDecimal("20.00"), 2)));

        when(pagarmeGateway.processPix(any(), any(), any(), any(), any(), any()))
                .thenReturn(PaymentTransactionResult.pix("tx_pix_999", new BigDecimal("100.00"), "qr_url", "copia_cola_123"));

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        EcommerceDto.ShippingAddressDto addr = new EcommerceDto.ShippingAddressDto(
                "Rua A", "10", null, "Bairro", "Cidade", "SP", "01001-000");
        EcommerceDto.CheckoutRequest request = new EcommerceDto.CheckoutRequest(
                "sess_1", null, "Cliente", "cliente@email.com", "123.456.789-00",
                null, addr, ShippingCarrier.CORREIOS, ShippingService.SEDEX,
                EcommercePaymentMethod.PIX, 1, null, "Obs");

        Order order = ecommerceService.checkout(tenantId, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.AGUARDANDO_PAGAMENTO);
        assertThat(order.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(stock.getReservedQuantity()).isEqualTo(1); // Estoque reservado para PIX
        verify(orderRepository).save(any(Order.class));
        verify(stockItemRepository).save(stock);
    }

    @Test
    @DisplayName("Deve realizar Checkout com Cartão de Crédito aprovando pagamento e baixando estoque imediatamente")
    void shouldCheckoutWithCreditCardApproved() {
        Cart cart = Cart.createNew(tenantId, "sess_1", null);
        cart.addItem(skuId, "SKU-1", "Livro", 1, new BigDecimal("100.00"));
        when(cartRepository.findByTenantIdAndSessionId(tenantId, "sess_1")).thenReturn(Optional.of(cart));

        CompanyJpaEntity matriz = CompanyJpaEntity.builder().id(companyId).name("Matriz").headquarters(true).build();
        when(companyRepository.findAllByTenantId(tenantId)).thenReturn(List.of(matriz));

        StockItemJpaEntity stock = StockItemJpaEntity.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .companyId(companyId).skuId(skuId).quantity(10).reservedQuantity(0).build();
        when(stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId)).thenReturn(Optional.of(stock));

        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).skuCode("SKU-1").price(new BigDecimal("100.00")).weightGrams(400).build();
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));

        when(shippingCalculator.calculate(any(), any(), any(), anyInt(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of(ShippingQuote.available(ShippingCarrier.CORREIOS, ShippingService.SEDEX, "SEDEX", BigDecimal.ZERO, 1)));

        when(pagarmeGateway.processCreditCard(any(), any(), any(), anyInt(), any(), any(), any(), any()))
                .thenReturn(PaymentTransactionResult.creditCard("tx_card_123", PaymentStatus.PAGO, new BigDecimal("100.00"), 1, "VISA", "1234"));

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        EcommerceDto.ShippingAddressDto addr = new EcommerceDto.ShippingAddressDto(
                "Rua B", "20", null, "Bairro", "Cidade", "SP", "01001-000");
        EcommerceDto.CheckoutRequest request = new EcommerceDto.CheckoutRequest(
                "sess_1", null, "Cliente", "cliente@email.com", "123.456.789-00",
                null, addr, ShippingCarrier.CORREIOS, ShippingService.SEDEX,
                EcommercePaymentMethod.CREDIT_CARD, 1, "tok_visa_1234", null);

        Order order = ecommerceService.checkout(tenantId, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAGO);
        assertThat(stock.getQuantity()).isEqualTo(9); // Baixa direta no estoque
        verify(stockItemRepository).save(stock);
    }

    @Test
    @DisplayName("Webhook Idempotency: Não deve reprocessar webhook se já tiver sido recebido anteriormente")
    void shouldDiscardDuplicateWebhookWithIdempotency() {
        when(webhookRepository.isAlreadyProcessed(tenantId, "PAGARME", "evt_duplicate")).thenReturn(true);

        ecommerceService.processPagarmeWebhook(tenantId, "evt_duplicate", "order.paid", "{}");

        verify(orderRepository, never()).findAllByTenantId(any(), anyInt(), anyInt());
        verify(webhookRepository, never()).markAsProcessed(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve despachar pedido e atualizar código de rastreamento")
    void shouldShipOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.create(tenantId, "PED-1", null, "Cliente", "c@email.com", "123", null, companyId,
                BigDecimal.ZERO, BigDecimal.ZERO, ShippingCarrier.CORREIOS, ShippingService.SEDEX, null,
                List.of(new com.livraria.ecommerce.domain.model.ecommerce.OrderItem(UUID.randomUUID(), skuId, "SKU-1", "Livro", 1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"), "49019900", "5102")), null);
        order.markAsPaid("tx_1");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order shipped = ecommerceService.shipOrder(tenantId, orderId, "SEDEX123", "http://rastreio");
        assertThat(shipped.getStatus()).isEqualTo(OrderStatus.ENVIADO);
        assertThat(shipped.getTrackingCode()).isEqualTo("SEDEX123");
    }
}
