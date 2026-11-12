package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.application.dto.ecommerce.EcommerceDto;
import com.livraria.ecommerce.domain.model.ecommerce.Cart;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingQuote;

import java.util.List;
import java.util.UUID;

public interface EcommerceUseCase {

    Cart getOrCreateCart(UUID tenantId, String sessionId, UUID customerId);

    Cart addItemToCart(UUID tenantId, String sessionId, UUID customerId, UUID skuId, int quantity);

    Cart updateCartItem(UUID tenantId, String sessionId, UUID customerId, UUID skuId, int quantity);

    Cart removeCartItem(UUID tenantId, String sessionId, UUID customerId, UUID skuId);

    void clearCart(UUID tenantId, String sessionId, UUID customerId);

    List<ShippingQuote> calculateShipping(UUID tenantId, String destinationZip, String sessionId, UUID customerId);

    Order checkout(UUID tenantId, EcommerceDto.CheckoutRequest request);

    void processPagarmeWebhook(UUID tenantId, String eventId, String eventType, String payload);

    Order getOrder(UUID tenantId, UUID orderId);

    List<Order> listOrders(UUID tenantId, int page, int size);

    Order shipOrder(UUID tenantId, UUID orderId, String trackingCode, String trackingUrl);

    Order deliverOrder(UUID tenantId, UUID orderId);

    Order cancelOrder(UUID tenantId, UUID orderId, String reason);
}

