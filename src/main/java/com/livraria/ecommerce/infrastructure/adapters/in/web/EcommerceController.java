package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.ecommerce.EcommerceDto;
import com.livraria.ecommerce.domain.model.ecommerce.Cart;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingQuote;
import com.livraria.ecommerce.domain.port.in.EcommerceUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ecommerce")
@Tag(name = "E-Commerce", description = "Carrinho, Cotação de Frete, Checkout Pagar.me V5 e Gestão de Pedidos")
public class EcommerceController {

    private final EcommerceUseCase ecommerceUseCase;

    public EcommerceController(EcommerceUseCase ecommerceUseCase) {
        this.ecommerceUseCase = ecommerceUseCase;
    }

    private UUID getTenantId(String headerTenantId) {
        if (headerTenantId != null && !headerTenantId.isBlank()) {
            return UUID.fromString(headerTenantId);
        }
        return TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @GetMapping("/cart")
    @Operation(summary = "Obter ou inicializar carrinho de compras")
    public ResponseEntity<EcommerceDto.CartResponse> getCart(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "customerId", required = false) UUID customerId) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Cart cart = ecommerceUseCase.getOrCreateCart(tenantId, sessionId, customerId);
        return ResponseEntity.ok(toCartResponse(cart));
    }

    @PostMapping("/cart/items")
    @Operation(summary = "Adicionar item ao carrinho de compras")
    public ResponseEntity<EcommerceDto.CartResponse> addItemToCart(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @Valid @RequestBody EcommerceDto.AddCartItemRequest request) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Cart cart = ecommerceUseCase.addItemToCart(tenantId, sessionId, customerId, request.skuId(), request.quantity());
        return ResponseEntity.ok(toCartResponse(cart));
    }

    @PutMapping("/cart/items/{skuId}")
    @Operation(summary = "Atualizar quantidade de um item no carrinho")
    public ResponseEntity<EcommerceDto.CartResponse> updateCartItem(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @PathVariable UUID skuId,
            @Valid @RequestBody EcommerceDto.UpdateCartItemRequest request) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Cart cart = ecommerceUseCase.updateCartItem(tenantId, sessionId, customerId, skuId, request.quantity());
        return ResponseEntity.ok(toCartResponse(cart));
    }

    @DeleteMapping("/cart/items/{skuId}")
    @Operation(summary = "Remover item do carrinho de compras")
    public ResponseEntity<EcommerceDto.CartResponse> removeCartItem(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @PathVariable UUID skuId) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Cart cart = ecommerceUseCase.removeCartItem(tenantId, sessionId, customerId, skuId);
        return ResponseEntity.ok(toCartResponse(cart));
    }

    @DeleteMapping("/cart")
    @Operation(summary = "Esvaziar carrinho de compras")
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam(value = "sessionId", required = false) String sessionId,
            @RequestParam(value = "customerId", required = false) UUID customerId) {
        UUID tenantId = getTenantId(tenantIdHeader);
        ecommerceUseCase.clearCart(tenantId, sessionId, customerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/shipping/calculate")
    @Operation(summary = "Calcular frete multi-transportadora (Correios e Melhor Envio com cache Redis)")
    public ResponseEntity<List<EcommerceDto.ShippingQuoteResponse>> calculateShipping(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @Valid @RequestBody EcommerceDto.CalculateShippingRequest request) {
        UUID tenantId = getTenantId(tenantIdHeader);
        List<ShippingQuote> quotes = ecommerceUseCase.calculateShipping(
                tenantId, request.destinationZip(), request.sessionId(), request.customerId());
        List<EcommerceDto.ShippingQuoteResponse> response = quotes.stream()
                .map(q -> new EcommerceDto.ShippingQuoteResponse(
                        q.carrier(), q.service(), q.name(), q.price(), q.deliveryDays(), q.isAvailable(), q.error()))
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout")
    @Operation(summary = "Realizar Checkout transparente com Pagar.me V5 (PIX, Cartão Tokenizado ou Boleto)")
    public ResponseEntity<EcommerceDto.OrderResponse> checkout(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @Valid @RequestBody EcommerceDto.CheckoutRequest request) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Order order = ecommerceUseCase.checkout(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toOrderResponse(order));
    }

    @GetMapping("/orders")
    @Operation(summary = "Listar pedidos do e-commerce")
    public ResponseEntity<List<EcommerceDto.OrderResponse>> listOrders(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = getTenantId(tenantIdHeader);
        List<Order> orders = ecommerceUseCase.listOrders(tenantId, page, size);
        return ResponseEntity.ok(orders.stream().map(this::toOrderResponse).toList());
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Obter detalhes de um pedido pelo ID")
    public ResponseEntity<EcommerceDto.OrderResponse> getOrder(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Order order = ecommerceUseCase.getOrder(tenantId, id);
        return ResponseEntity.ok(toOrderResponse(order));
    }

    @PostMapping("/orders/{id}/ship")
    @Operation(summary = "Despachar pedido informando código de rastreio")
    public ResponseEntity<EcommerceDto.OrderResponse> shipOrder(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody EcommerceDto.ShipOrderRequest request) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Order order = ecommerceUseCase.shipOrder(tenantId, id, request.trackingCode(), request.trackingUrl());
        return ResponseEntity.ok(toOrderResponse(order));
    }

    @PostMapping("/orders/{id}/deliver")
    @Operation(summary = "Confirmar entrega do pedido ao cliente")
    public ResponseEntity<EcommerceDto.OrderResponse> deliverOrder(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Order order = ecommerceUseCase.deliverOrder(tenantId, id);
        return ResponseEntity.ok(toOrderResponse(order));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancelar pedido e estornar estoque")
    public ResponseEntity<EcommerceDto.OrderResponse> cancelOrder(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody EcommerceDto.CancelOrderRequest request) {
        UUID tenantId = getTenantId(tenantIdHeader);
        Order order = ecommerceUseCase.cancelOrder(tenantId, id, request.reason());
        return ResponseEntity.ok(toOrderResponse(order));
    }

    private EcommerceDto.CartResponse toCartResponse(Cart cart) {
        List<EcommerceDto.CartItemResponse> items = cart.getItems().stream().map(i ->
                new EcommerceDto.CartItemResponse(
                        i.getId(),
                        i.getSkuId(),
                        i.getSkuCode(),
                        i.getProductTitle(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTotal()
                )).toList();
        return new EcommerceDto.CartResponse(
                cart.getId(),
                cart.getSessionId(),
                cart.getCustomerId(),
                items,
                cart.calculateSubtotal(),
                cart.calculateTotalItems(),
                cart.getUpdatedAt()
        );
    }

    private EcommerceDto.OrderResponse toOrderResponse(Order order) {
        List<EcommerceDto.OrderItemResponse> items = order.getItems().stream().map(i ->
                new EcommerceDto.OrderItemResponse(
                        i.getId(),
                        i.getSkuId(),
                        i.getSkuCode(),
                        i.getProductTitle(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getDiscount(),
                        i.getTotal(),
                        i.getNcm(),
                        i.getCfop()
                )).toList();

        List<EcommerceDto.OrderPaymentResponse> payments = order.getPayments().stream().map(p ->
                new EcommerceDto.OrderPaymentResponse(
                        p.getId(),
                        p.getPaymentMethod(),
                        p.getStatus(),
                        p.getAmount(),
                        p.getInstallments(),
                        p.getPixQrCode(),
                        p.getPixCopiaCola(),
                        p.getBoletoBarcode(),
                        p.getBoletoUrl(),
                        p.getBoletoDueDate(),
                        p.getCardBrand(),
                        p.getCardLastFour()
                )).toList();

        String addressFormatted = order.getShippingAddress() != null ? order.getShippingAddress().formatted() : "";

        return new EcommerceDto.OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerDocument(),
                order.getCustomerPhone(),
                order.getStatus(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingAmount(),
                order.getTotalAmount(),
                order.getShippingCarrier(),
                order.getShippingService(),
                order.getTrackingCode(),
                order.getTrackingUrl(),
                addressFormatted,
                items,
                payments,
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getCanceledAt()
        );
    }
}
