package com.livraria.ecommerce.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.application.dto.ecommerce.EcommerceDto;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.ecommerce.Cart;
import com.livraria.ecommerce.domain.model.ecommerce.CartItem;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderItem;
import com.livraria.ecommerce.domain.model.ecommerce.OrderPayment;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentGateway;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentStatus;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentTransactionResult;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingAddress;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingQuote;
import com.livraria.ecommerce.domain.model.stock.MovementType;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import com.livraria.ecommerce.domain.port.in.EcommerceUseCase;
import com.livraria.ecommerce.domain.port.out.CartRepositoryPort;
import com.livraria.ecommerce.domain.port.out.OrderRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PagarmeGatewayPort;
import com.livraria.ecommerce.domain.port.out.ShippingCalculatorPort;
import com.livraria.ecommerce.domain.port.out.WebhookEventRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockMovementJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class EcommerceService implements EcommerceUseCase {

    private static final Logger log = LoggerFactory.getLogger(EcommerceService.class);

    private final CartRepositoryPort cartRepository;
    private final OrderRepositoryPort orderRepository;
    private final WebhookEventRepositoryPort webhookRepository;
    private final PagarmeGatewayPort pagarmeGateway;
    private final ShippingCalculatorPort shippingCalculator;
    private final SpringDataSkuRepository skuRepository;
    private final SpringDataProductRepository productRepository;
    private final SpringDataStockItemRepository stockItemRepository;
    private final SpringDataStockMovementRepository stockMovementRepository;
    private final SpringDataCompanyRepository companyRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final AuditUseCase auditUseCase;

    @Autowired
    public EcommerceService(CartRepositoryPort cartRepository,
                            OrderRepositoryPort orderRepository,
                            WebhookEventRepositoryPort webhookRepository,
                            PagarmeGatewayPort pagarmeGateway,
                            ShippingCalculatorPort shippingCalculator,
                            SpringDataSkuRepository skuRepository,
                            SpringDataProductRepository productRepository,
                            SpringDataStockItemRepository stockItemRepository,
                            SpringDataStockMovementRepository stockMovementRepository,
                            SpringDataCompanyRepository companyRepository,
                            @Autowired(required = false) RabbitTemplate rabbitTemplate,
                            ObjectMapper objectMapper,
                            @Autowired(required = false) AuditUseCase auditUseCase) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.webhookRepository = webhookRepository;
        this.pagarmeGateway = pagarmeGateway;
        this.shippingCalculator = shippingCalculator;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.stockItemRepository = stockItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.companyRepository = companyRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.auditUseCase = auditUseCase;
    }

    public EcommerceService(CartRepositoryPort cartRepository,
                            OrderRepositoryPort orderRepository,
                            WebhookEventRepositoryPort webhookRepository,
                            PagarmeGatewayPort pagarmeGateway,
                            ShippingCalculatorPort shippingCalculator,
                            SpringDataSkuRepository skuRepository,
                            SpringDataProductRepository productRepository,
                            SpringDataStockItemRepository stockItemRepository,
                            SpringDataStockMovementRepository stockMovementRepository,
                            SpringDataCompanyRepository companyRepository,
                            RabbitTemplate rabbitTemplate,
                            ObjectMapper objectMapper) {
        this(cartRepository, orderRepository, webhookRepository, pagarmeGateway, shippingCalculator, skuRepository, productRepository, stockItemRepository, stockMovementRepository, companyRepository, rabbitTemplate, objectMapper, null);
    }

    @Override
    @Transactional
    public Cart getOrCreateCart(UUID tenantId, String sessionId, UUID customerId) {
        Optional<Cart> existing = Optional.empty();
        if (customerId != null) {
            existing = cartRepository.findByTenantIdAndCustomerId(tenantId, customerId);
        }
        if (existing.isEmpty() && sessionId != null && !sessionId.isBlank()) {
            existing = cartRepository.findByTenantIdAndSessionId(tenantId, sessionId);
        }
        return existing.orElseGet(() -> {
            Cart newCart = Cart.createNew(tenantId, sessionId, customerId);
            return cartRepository.save(newCart);
        });
    }

    @Override
    @Transactional
    public Cart addItemToCart(UUID tenantId, String sessionId, UUID customerId, UUID skuId, int quantity) {
        Cart cart = getOrCreateCart(tenantId, sessionId, customerId);
        SkuJpaEntity sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException("SKU não encontrado com ID: " + skuId));
        ProductJpaEntity product = productRepository.findById(sku.getProductId())
                .orElseThrow(() -> new BusinessException("Produto não encontrado para o SKU: " + skuId));

        cart.addItem(sku.getId(), sku.getSkuCode(), product.getTitle(), quantity, sku.getPrice());
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart updateCartItem(UUID tenantId, String sessionId, UUID customerId, UUID skuId, int quantity) {
        Cart cart = getOrCreateCart(tenantId, sessionId, customerId);
        cart.updateItemQuantity(skuId, quantity);
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart removeCartItem(UUID tenantId, String sessionId, UUID customerId, UUID skuId) {
        Cart cart = getOrCreateCart(tenantId, sessionId, customerId);
        cart.removeItem(skuId);
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(UUID tenantId, String sessionId, UUID customerId) {
        Cart cart = getOrCreateCart(tenantId, sessionId, customerId);
        cart.clear();
        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShippingQuote> calculateShipping(UUID tenantId, String destinationZip, String sessionId, UUID customerId) {
        Cart cart = getOrCreateCart(tenantId, sessionId, customerId);
        if (cart.isEmpty()) {
            throw new BusinessException("Carrinho vazio. Adicione itens antes de calcular o frete.");
        }

        // Buscar CEP de origem da Matriz ou empresa padrão do tenant
        String originZip = companyRepository.findAllByTenantId(tenantId).stream()
                .filter(CompanyJpaEntity::isHeadquarters)
                .findFirst()
                .map(c -> "01001-000")
                .orElse("01001-000");

        int totalWeight = 0;
        double maxHeight = 23.0, maxWidth = 16.0, totalDepth = 0;

        for (CartItem item : cart.getItems()) {
            SkuJpaEntity sku = skuRepository.findById(item.getSkuId()).orElse(null);
            if (sku != null) {
                totalWeight += (sku.getWeightGrams() * item.getQuantity());
                totalDepth += (2.5 * item.getQuantity());
            }
        }

        return shippingCalculator.calculate(tenantId, originZip, destinationZip,
                totalWeight, maxHeight, maxWidth, totalDepth, cart.calculateSubtotal());
    }

    @Override
    @Transactional
    public Order checkout(UUID tenantId, EcommerceDto.CheckoutRequest request) {
        Cart cart = getOrCreateCart(tenantId, request.sessionId(), request.customerId());
        if (cart.isEmpty()) {
            throw new BusinessException("Não é possível realizar checkout de um carrinho vazio.");
        }

        // Determinar Empresa de Origem (Matriz de fulfillment e-commerce)
        CompanyJpaEntity fulfillmentCompany = companyRepository.findAllByTenantId(tenantId).stream()
                .filter(CompanyJpaEntity::isHeadquarters)
                .findFirst()
                .orElseGet(() -> companyRepository.findAllByTenantId(tenantId).stream().findFirst()
                        .orElseThrow(() -> new BusinessException("Nenhuma empresa configurada para expedição.")));

        // Validar estoque disponível para todos os itens
        for (CartItem item : cart.getItems()) {
            StockItemJpaEntity stock = stockItemRepository
                    .findByTenantIdAndCompanyIdAndSkuId(tenantId, fulfillmentCompany.getId(), item.getSkuId())
                    .orElseThrow(() -> new BusinessException("Item " + item.getProductTitle() + " sem estoque disponível para expedição."));
            if (stock.getQuantity() - stock.getReservedQuantity() < item.getQuantity()) {
                throw new BusinessException("Estoque insuficiente para o produto: " + item.getProductTitle());
            }
        }

        // Calcular frete para o checkout
        List<ShippingQuote> quotes = calculateShipping(tenantId, request.shippingAddress().zipCode(),
                request.sessionId(), request.customerId());
        ShippingQuote selectedQuote = quotes.stream()
                .filter(q -> q.carrier() == request.shippingCarrier() && q.service() == request.shippingService())
                .findFirst()
                .orElse(quotes.getFirst());

        BigDecimal shippingAmount = selectedQuote.price();

        // Construir itens do pedido
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            SkuJpaEntity sku = skuRepository.findById(item.getSkuId()).orElseThrow();
            orderItems.add(new OrderItem(
                    UUID.randomUUID(),
                    item.getSkuId(),
                    item.getSkuCode(),
                    item.getProductTitle(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    BigDecimal.ZERO,
                    item.getTotal(),
                    sku.getNcm() != null ? sku.getNcm() : "4901.99.00",
                    sku.getCfop() != null ? sku.getCfop() : "5102"
            ));
        }

        String orderNumber = "PED-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 9999));

        ShippingAddress address = new ShippingAddress(
                request.shippingAddress().street(),
                request.shippingAddress().number(),
                request.shippingAddress().complement(),
                request.shippingAddress().neighborhood(),
                request.shippingAddress().city(),
                request.shippingAddress().state(),
                request.shippingAddress().zipCode()
        );

        Order order = Order.create(tenantId, orderNumber, request.customerId(),
                request.customerName(), request.customerEmail(), request.customerDocument(),
                request.customerPhone(), fulfillmentCompany.getId(), BigDecimal.ZERO,
                shippingAmount, request.shippingCarrier(), request.shippingService(),
                address, orderItems, request.notes());

        // Processar Pagamento via Gateway Pagar.me V5
        PaymentTransactionResult txResult;
        switch (request.paymentMethod()) {
            case PIX -> txResult = pagarmeGateway.processPix(tenantId, orderNumber, order.getTotalAmount(),
                    request.customerName(), request.customerEmail(), request.customerDocument());
            case CREDIT_CARD -> {
                int installments = request.installments() != null && request.installments() > 0 ? request.installments() : 1;
                txResult = pagarmeGateway.processCreditCard(tenantId, orderNumber, order.getTotalAmount(),
                        installments, request.cardToken(), request.customerName(),
                        request.customerEmail(), request.customerDocument());
            }
            case BOLETO -> txResult = pagarmeGateway.processBoleto(tenantId, orderNumber, order.getTotalAmount(),
                    3, request.customerName(), request.customerEmail(), request.customerDocument());
            default -> throw new BusinessException("Método de pagamento não suportado para checkout e-commerce: " + request.paymentMethod());
        }

        OrderPayment payment = new OrderPayment(
                UUID.randomUUID(),
                request.paymentMethod(),
                PaymentGateway.PAGARME,
                txResult.gatewayTransactionId(),
                txResult.status(),
                txResult.amount(),
                txResult.installments(),
                txResult.pixQrCode(),
                txResult.pixCopiaCola(),
                txResult.boletoBarcode(),
                txResult.boletoUrl(),
                txResult.boletoDueDate(),
                txResult.cardBrand(),
                txResult.cardLastFour(),
                Instant.now(),
                Instant.now(),
                txResult.status() == PaymentStatus.PAGO ? Instant.now() : null
        );
        order.addPayment(payment);

        // Se pago imediatamente (ex: cartão aprovado), atualiza status e dá baixa no estoque
        if (payment.getStatus() == PaymentStatus.PAGO) {
            order.markAsPaid(txResult.gatewayTransactionId());
            decrementStockForOrder(tenantId, fulfillmentCompany.getId(), order);
        } else {
            // Reserva temporária de estoque para PIX/Boleto
            reserveStockForOrder(tenantId, fulfillmentCompany.getId(), order);
        }

        orderRepository.save(order);

        if (auditUseCase != null) {
            auditUseCase.recordEvent("ORDER_CREATED", "ORDER", order.getId().toString(), Map.of(
                    "orderNumber", order.getOrderNumber(),
                    "totalAmount", order.getTotalAmount(),
                    "status", order.getStatus().name(),
                    "itemsCount", order.getItems().size()
            ));
        }

        // Limpar carrinho após checkout bem-sucedido
        cart.clear();
        cartRepository.save(cart);

        log.info("Checkout concluído com sucesso: order={}, status={}, total={}",
                order.getOrderNumber(), order.getStatus(), order.getTotalAmount());
        return order;
    }

    @Override
    @Transactional
    public void processPagarmeWebhook(UUID tenantId, String eventId, String eventType, String payload) {
        log.info("Webhook Pagar.me recebido: eventId={}, type={}", eventId, eventType);

        // 1. Garantir idempotência estrita
        if (webhookRepository.isAlreadyProcessed(tenantId, "PAGARME", eventId)) {
            log.info("Webhook {} já processado anteriormente. Descartando com idempotência.", eventId);
            return;
        }

        // 2. Extrair dados do payload
        try {
            JsonNode root = objectMapper.readTree(payload);
            String txId = null;
            String status = null;

            if (root.has("data")) {
                JsonNode data = root.get("data");
                if (data.has("id")) {
                    txId = data.get("id").asText();
                }
                if (data.has("status")) {
                    status = data.get("status").asText();
                }
                // Se vier de uma charge
                if (data.has("charges") && data.get("charges").isArray() && !data.get("charges").isEmpty()) {
                    JsonNode firstCharge = data.get("charges").get(0);
                    if (firstCharge.has("last_transaction") && firstCharge.get("last_transaction").has("id")) {
                        txId = firstCharge.get("last_transaction").get("id").asText();
                    }
                    if (firstCharge.has("status")) {
                        status = firstCharge.get("status").asText();
                    }
                }
            }

            if ("order.paid".equalsIgnoreCase(eventType) || "charge.paid".equalsIgnoreCase(eventType) || "paid".equalsIgnoreCase(status)) {
                if (txId != null) {
                    // Localizar pagamento e pedido associado
                    final String finalTxId = txId;
                    List<Order> orders = orderRepository.findAllByTenantId(tenantId, 0, 100);
                    for (Order order : orders) {
                        for (OrderPayment payment : order.getPayments()) {
                            if (finalTxId.equals(payment.getGatewayTransactionId()) || order.getOrderNumber().equals(finalTxId)) {
                                if (order.getStatus() == OrderStatus.AGUARDANDO_PAGAMENTO) {
                                    order.markAsPaid(finalTxId);
                                    // Liberar reserva e debitar do saldo real
                                    releaseReservationAndDecrementStock(tenantId, order.getOriginCompanyId(), order);
                                    orderRepository.save(order);
                                    log.info("Pedido {} marcado como PAGO via webhook Pagar.me", order.getOrderNumber());

                                    if (auditUseCase != null) {
                                        auditUseCase.recordEvent(
                                                null,
                                                "webhook@pagar.me",
                                                "INTEGRATION_GATEWAY",
                                                "127.0.0.1",
                                                "ORDER_STATUS_GATEWAY_PAID",
                                                "ORDER",
                                                order.getId().toString(),
                                                Map.of(
                                                        "orderNumber", order.getOrderNumber(),
                                                        "gatewayTxId", finalTxId,
                                                        "eventType", eventType,
                                                        "totalAmount", order.getTotalAmount()
                                                )
                                        );
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }

            // 3. Registrar como processado
            webhookRepository.markAsProcessed(tenantId, "PAGARME", eventId, eventType, payload);

            // 4. Se RabbitMQ estiver disponível, publicar evento assíncrono para emissão de NF-e
            if (rabbitTemplate != null) {
                try {
                    rabbitTemplate.convertAndSend("order.exchange", "order.payment.approved", payload);
                } catch (Exception e) {
                    log.warn("RabbitMQ offline, notificação assíncrona postergada: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Erro ao processar payload do webhook Pagar.me: {}", e.getMessage(), e);
            throw new BusinessException("Falha ao decodificar webhook Pagar.me: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrder(UUID tenantId, UUID orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("Pedido não encontrado com ID: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> listOrders(UUID tenantId, int page, int size) {
        return orderRepository.findAllByTenantId(tenantId, page, size);
    }

    @Override
    @Transactional
    public Order shipOrder(UUID tenantId, UUID orderId, String trackingCode, String trackingUrl) {
        Order order = getOrder(tenantId, orderId);
        order.ship(trackingCode, trackingUrl);
        Order saved = orderRepository.save(order);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("ORDER_STATUS_SHIPPED", "ORDER", saved.getId().toString(), Map.of(
                    "orderNumber", saved.getOrderNumber(),
                    "trackingCode", trackingCode,
                    "trackingUrl", trackingUrl != null ? trackingUrl : ""
            ));
        }
        return saved;
    }

    @Override
    @Transactional
    public Order deliverOrder(UUID tenantId, UUID orderId) {
        Order order = getOrder(tenantId, orderId);
        order.deliver();
        Order saved = orderRepository.save(order);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("ORDER_STATUS_DELIVERED", "ORDER", saved.getId().toString(), Map.of(
                    "orderNumber", saved.getOrderNumber()
            ));
        }
        return saved;
    }

    @Override
    @Transactional
    public Order cancelOrder(UUID tenantId, UUID orderId, String reason) {
        Order order = getOrder(tenantId, orderId);
        OrderStatus oldStatus = order.getStatus();
        order.cancel(reason);

        // Se estava pago ou com estoque reservado, devolver ao estoque
        if (oldStatus == OrderStatus.PAGO || oldStatus == OrderStatus.FATURADO_A_PRAZO) {
            restockOrder(tenantId, order.getOriginCompanyId(), order);
        } else if (oldStatus == OrderStatus.AGUARDANDO_PAGAMENTO) {
            cancelReservationForOrder(tenantId, order.getOriginCompanyId(), order);
        }

        Order saved = orderRepository.save(order);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("ORDER_STATUS_CANCELLED", "ORDER", saved.getId().toString(), Map.of(
                    "orderNumber", saved.getOrderNumber(),
                    "reason", reason,
                    "previousStatus", oldStatus.name()
            ));
        }
        return saved;
    }

    // --- Auxiliares de Estoque ---

    private void decrementStockForOrder(UUID tenantId, UUID companyId, Order order) {
        for (OrderItem item : order.getItems()) {
            StockItemJpaEntity stock = stockItemRepository
                    .findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, item.getSkuId())
                    .orElseThrow(() -> new BusinessException("Estoque não localizado para SKU: " + item.getSkuCode()));
            stock.setQuantity(stock.getQuantity() - item.getQuantity());
            stock.setUpdatedAt(Instant.now());
            stockItemRepository.save(stock);

            StockMovementJpaEntity movement = StockMovementJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .companyId(companyId)
                    .skuId(item.getSkuId())
                    .movementType(MovementType.VENDA_ECOMMERCE.name())
                    .quantityChange(-item.getQuantity())
                    .balanceAfter(stock.getQuantity())
                    .referenceId(order.getId())
                    .reason("Venda E-commerce Pedido " + order.getOrderNumber())
                    .createdAt(Instant.now())
                    .build();
            stockMovementRepository.save(movement);
        }
    }

    private void reserveStockForOrder(UUID tenantId, UUID companyId, Order order) {
        for (OrderItem item : order.getItems()) {
            StockItemJpaEntity stock = stockItemRepository
                    .findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, item.getSkuId())
                    .orElseThrow(() -> new BusinessException("Estoque não localizado para SKU: " + item.getSkuCode()));
            stock.setReservedQuantity(stock.getReservedQuantity() + item.getQuantity());
            stock.setUpdatedAt(Instant.now());
            stockItemRepository.save(stock);

            StockMovementJpaEntity movement = StockMovementJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .companyId(companyId)
                    .skuId(item.getSkuId())
                    .movementType(MovementType.RESERVA.name())
                    .quantityChange(item.getQuantity())
                    .balanceAfter(stock.getQuantity() - stock.getReservedQuantity())
                    .referenceId(order.getId())
                    .reason("Reserva para Pedido E-commerce " + order.getOrderNumber())
                    .createdAt(Instant.now())
                    .build();
            stockMovementRepository.save(movement);
        }
    }

    private void releaseReservationAndDecrementStock(UUID tenantId, UUID companyId, Order order) {
        for (OrderItem item : order.getItems()) {
            StockItemJpaEntity stock = stockItemRepository
                    .findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, item.getSkuId())
                    .orElseThrow();
            stock.setReservedQuantity(Math.max(0, stock.getReservedQuantity() - item.getQuantity()));
            stock.setQuantity(stock.getQuantity() - item.getQuantity());
            stock.setUpdatedAt(Instant.now());
            stockItemRepository.save(stock);

            StockMovementJpaEntity movement = StockMovementJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .companyId(companyId)
                    .skuId(item.getSkuId())
                    .movementType(MovementType.VENDA_ECOMMERCE.name())
                    .quantityChange(-item.getQuantity())
                    .balanceAfter(stock.getQuantity())
                    .referenceId(order.getId())
                    .reason("Compensação de Pagamento Pedido " + order.getOrderNumber())
                    .createdAt(Instant.now())
                    .build();
            stockMovementRepository.save(movement);
        }
    }

    private void cancelReservationForOrder(UUID tenantId, UUID companyId, Order order) {
        for (OrderItem item : order.getItems()) {
            StockItemJpaEntity stock = stockItemRepository
                    .findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, item.getSkuId())
                    .orElse(null);
            if (stock != null) {
                stock.setReservedQuantity(Math.max(0, stock.getReservedQuantity() - item.getQuantity()));
                stock.setUpdatedAt(Instant.now());
                stockItemRepository.save(stock);
            }
        }
    }

    private void restockOrder(UUID tenantId, UUID companyId, Order order) {
        for (OrderItem item : order.getItems()) {
            StockItemJpaEntity stock = stockItemRepository
                    .findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, item.getSkuId())
                    .orElse(null);
            if (stock != null) {
                stock.setQuantity(stock.getQuantity() + item.getQuantity());
                stock.setUpdatedAt(Instant.now());
                stockItemRepository.save(stock);

                StockMovementJpaEntity movement = StockMovementJpaEntity.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .companyId(companyId)
                        .skuId(item.getSkuId())
                        .movementType(MovementType.ENTRADA_AVULSA.name())
                        .quantityChange(item.getQuantity())
                        .balanceAfter(stock.getQuantity())
                        .referenceId(order.getId())
                        .reason("Estorno / Cancelamento Pedido " + order.getOrderNumber())
                        .createdAt(Instant.now())
                        .build();
                stockMovementRepository.save(movement);
            }
        }
    }
}
