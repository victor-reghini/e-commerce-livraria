package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livraria.ecommerce.application.dto.ecommerce.EcommerceDto;
import com.livraria.ecommerce.domain.model.ecommerce.Cart;
import com.livraria.ecommerce.domain.model.ecommerce.EcommercePaymentMethod;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderItem;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingAddress;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingCarrier;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingQuote;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingService;
import com.livraria.ecommerce.domain.port.in.EcommerceUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - EcommerceController")
class EcommerceControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private EcommerceUseCase ecommerceUseCase;

    @InjectMocks
    private EcommerceController ecommerceController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID skuId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ecommerceController).build();
    }

    @Test
    @DisplayName("GET /api/v1/ecommerce/cart - Deve retornar carrinho de compras")
    void shouldGetCart() throws Exception {
        Cart cart = Cart.createNew(tenantId, "sess_1", null);
        cart.addItem(skuId, "SKU-1", "Livro Clean Architecture", 2, new BigDecimal("89.90"));

        when(ecommerceUseCase.getOrCreateCart(eq(tenantId), eq("sess_1"), any())).thenReturn(cart);

        mockMvc.perform(get("/api/v1/ecommerce/cart")
                        .header("X-Tenant-ID", tenantId.toString())
                        .param("sessionId", "sess_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("sess_1"))
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.subtotal").value(179.80));
    }

    @Test
    @DisplayName("POST /api/v1/ecommerce/cart/items - Deve adicionar item ao carrinho")
    void shouldAddItemToCart() throws Exception {
        Cart cart = Cart.createNew(tenantId, "sess_1", null);
        cart.addItem(skuId, "SKU-1", "Livro", 1, new BigDecimal("50.00"));

        when(ecommerceUseCase.addItemToCart(eq(tenantId), eq("sess_1"), any(), eq(skuId), eq(1))).thenReturn(cart);

        EcommerceDto.AddCartItemRequest req = new EcommerceDto.AddCartItemRequest(skuId, 1);

        mockMvc.perform(post("/api/v1/ecommerce/cart/items")
                        .header("X-Tenant-ID", tenantId.toString())
                        .param("sessionId", "sess_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/ecommerce/cart/items/{skuId} - Deve atualizar quantidade")
    void shouldUpdateCartItem() throws Exception {
        Cart cart = Cart.createNew(tenantId, "sess_1", null);
        cart.addItem(skuId, "SKU-1", "Livro", 3, new BigDecimal("50.00"));

        when(ecommerceUseCase.updateCartItem(eq(tenantId), eq("sess_1"), any(), eq(skuId), eq(3))).thenReturn(cart);

        EcommerceDto.UpdateCartItemRequest req = new EcommerceDto.UpdateCartItemRequest(3);

        mockMvc.perform(put("/api/v1/ecommerce/cart/items/{skuId}", skuId)
                        .header("X-Tenant-ID", tenantId.toString())
                        .param("sessionId", "sess_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3));
    }

    @Test
    @DisplayName("DELETE /api/v1/ecommerce/cart - Deve esvaziar carrinho")
    void shouldClearCart() throws Exception {
        mockMvc.perform(delete("/api/v1/ecommerce/cart")
                        .header("X-Tenant-ID", tenantId.toString())
                        .param("sessionId", "sess_1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/ecommerce/shipping/calculate - Deve retornar cotações de frete")
    void shouldCalculateShipping() throws Exception {
        List<ShippingQuote> quotes = List.of(
                ShippingQuote.available(ShippingCarrier.CORREIOS, ShippingService.SEDEX, "Correios SEDEX", new BigDecimal("25.00"), 2),
                ShippingQuote.available(ShippingCarrier.MELHOR_ENVIO, ShippingService.MELHOR_ENVIO_ECONOMICO, "Melhor Envio Econômico", new BigDecimal("14.00"), 7)
        );

        when(ecommerceUseCase.calculateShipping(eq(tenantId), eq("01310100"), eq("sess_1"), any())).thenReturn(quotes);

        EcommerceDto.CalculateShippingRequest req = new EcommerceDto.CalculateShippingRequest("01310100", "sess_1", null);

        mockMvc.perform(post("/api/v1/ecommerce/shipping/calculate")
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Correios SEDEX"))
                .andExpect(jsonPath("$[0].price").value(25.00));
    }

    @Test
    @DisplayName("POST /api/v1/ecommerce/checkout - Deve realizar checkout e criar pedido")
    void shouldCheckout() throws Exception {
        OrderItem item = new OrderItem(UUID.randomUUID(), skuId, "SKU-1", "Livro", 1, new BigDecimal("80.00"), BigDecimal.ZERO, new BigDecimal("80.00"), "49019900", "5102");
        ShippingAddress addr = new ShippingAddress("Rua Teste", "123", null, "Bairro", "SP", "SP", "01001-000");
        Order order = Order.create(tenantId, "PED-20261112-0001", null, "Comprador", "comprador@email.com",
                "12345678900", null, UUID.randomUUID(), BigDecimal.ZERO, new BigDecimal("20.00"),
                ShippingCarrier.CORREIOS, ShippingService.SEDEX, addr, List.of(item), null);

        when(ecommerceUseCase.checkout(eq(tenantId), any())).thenReturn(order);

        EcommerceDto.ShippingAddressDto addrDto = new EcommerceDto.ShippingAddressDto("Rua Teste", "123", null, "Bairro", "SP", "SP", "01001-000");
        EcommerceDto.CheckoutRequest req = new EcommerceDto.CheckoutRequest(
                "sess_1", null, "Comprador", "comprador@email.com", "123.456.789-00",
                null, addrDto, ShippingCarrier.CORREIOS, ShippingService.SEDEX,
                EcommercePaymentMethod.PIX, 1, null, "Observação");

        mockMvc.perform(post("/api/v1/ecommerce/checkout")
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").value("PED-20261112-0001"))
                .andExpect(jsonPath("$.totalAmount").value(100.00));
    }

    @Test
    @DisplayName("POST /api/v1/ecommerce/orders/{id}/ship - Deve despachar pedido")
    void shouldShipOrder() throws Exception {
        OrderItem item = new OrderItem(UUID.randomUUID(), skuId, "SKU-1", "Livro", 1, new BigDecimal("80.00"), BigDecimal.ZERO, new BigDecimal("80.00"), "49019900", "5102");
        Order order = Order.create(tenantId, "PED-1", null, "Comprador", "comprador@email.com",
                "12345678900", null, UUID.randomUUID(), BigDecimal.ZERO, BigDecimal.ZERO,
                ShippingCarrier.CORREIOS, ShippingService.SEDEX, null, List.of(item), null);
        order.markAsPaid("tx_1");
        order.ship("SEDEX_TRACK_123", "http://rastreio");

        when(ecommerceUseCase.shipOrder(eq(tenantId), eq(orderId), eq("SEDEX_TRACK_123"), any())).thenReturn(order);

        EcommerceDto.ShipOrderRequest req = new EcommerceDto.ShipOrderRequest("SEDEX_TRACK_123", "http://rastreio");

        mockMvc.perform(post("/api/v1/ecommerce/orders/{id}/ship", orderId)
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingCode").value("SEDEX_TRACK_123"))
                .andExpect(jsonPath("$.status").value("ENVIADO"));
    }
}

