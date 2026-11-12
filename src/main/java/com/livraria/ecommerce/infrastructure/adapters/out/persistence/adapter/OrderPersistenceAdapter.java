package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

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
import com.livraria.ecommerce.domain.port.out.OrderRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.OrderItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.OrderJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.OrderPaymentJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataOrderItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataOrderPaymentRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository orderRepository;
    private final SpringDataOrderItemRepository orderItemRepository;
    private final SpringDataOrderPaymentRepository orderPaymentRepository;

    public OrderPersistenceAdapter(SpringDataOrderRepository orderRepository,
                                  SpringDataOrderItemRepository orderItemRepository,
                                  SpringDataOrderPaymentRepository orderPaymentRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPaymentRepository = orderPaymentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return orderRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findByTenantIdAndOrderNumber(UUID tenantId, String orderNumber) {
        return orderRepository.findByTenantIdAndOrderNumber(tenantId, orderNumber).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        return orderRepository.findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllByTenantId(UUID tenantId, int page, int size) {
        return orderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findAllByTenantIdAndStatus(UUID tenantId, OrderStatus status) {
        return orderRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, status.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasOverdueInvoices(UUID tenantId, String customerDocument) {
        if (customerDocument == null || customerDocument.isBlank()) {
            return false;
        }
        String cleanDoc = customerDocument.replaceAll("\\D", "");
        return orderRepository.hasOverdueInvoices(tenantId, cleanDoc, LocalDate.now());
    }

    @Override
    @Transactional
    public Order save(Order order) {
        ShippingAddress addr = order.getShippingAddress();
        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(order.getId())
                .tenantId(order.getTenantId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerDocument(order.getCustomerDocument())
                .customerPhone(order.getCustomerPhone())
                .originCompanyId(order.getOriginCompanyId())
                .status(order.getStatus().name())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingAmount(order.getShippingAmount())
                .totalAmount(order.getTotalAmount())
                .shippingCarrier(order.getShippingCarrier() != null ? order.getShippingCarrier().name() : null)
                .shippingService(order.getShippingService() != null ? order.getShippingService().name() : null)
                .trackingCode(order.getTrackingCode())
                .trackingUrl(order.getTrackingUrl())
                .shippingAddressStreet(addr != null ? addr.street() : null)
                .shippingAddressNumber(addr != null ? addr.number() : null)
                .shippingAddressComplement(addr != null ? addr.complement() : null)
                .shippingAddressNeighborhood(addr != null ? addr.neighborhood() : null)
                .shippingAddressCity(addr != null ? addr.city() : null)
                .shippingAddressState(addr != null ? addr.state() : null)
                .shippingAddressZipCode(addr != null ? addr.zipCode() : null)
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .paidAt(order.getPaidAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .canceledAt(order.getCanceledAt())
                .build();

        orderRepository.save(entity);

        // Salvar Itens
        for (OrderItem item : order.getItems()) {
            OrderItemJpaEntity itemEntity = OrderItemJpaEntity.builder()
                    .id(item.getId())
                    .tenantId(order.getTenantId())
                    .order(entity)
                    .skuId(item.getSkuId())
                    .skuCode(item.getSkuCode())
                    .productTitle(item.getProductTitle())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .discount(item.getDiscount())
                    .total(item.getTotal())
                    .ncm(item.getNcm())
                    .cfop(item.getCfop())
                    .build();
            orderItemRepository.save(itemEntity);
        }

        // Salvar Pagamentos
        for (OrderPayment p : order.getPayments()) {
            OrderPaymentJpaEntity paymentEntity = OrderPaymentJpaEntity.builder()
                    .id(p.getId())
                    .tenantId(order.getTenantId())
                    .order(entity)
                    .paymentMethod(p.getPaymentMethod().name())
                    .gateway(p.getGateway().name())
                    .gatewayTransactionId(p.getGatewayTransactionId())
                    .status(p.getStatus().name())
                    .amount(p.getAmount())
                    .installments(p.getInstallments())
                    .pixQrCode(p.getPixQrCode())
                    .pixCopiaCola(p.getPixCopiaCola())
                    .boletoBarcode(p.getBoletoBarcode())
                    .boletoUrl(p.getBoletoUrl())
                    .boletoDueDate(p.getBoletoDueDate())
                    .cardBrand(p.getCardBrand())
                    .cardLastFour(p.getCardLastFour())
                    .createdAt(p.getCreatedAt())
                    .updatedAt(p.getUpdatedAt())
                    .paidAt(p.getPaidAt())
                    .build();
            orderPaymentRepository.save(paymentEntity);
        }

        return order;
    }

    private Order toDomain(OrderJpaEntity entity) {
        List<OrderItemJpaEntity> itemEntities = orderItemRepository.findAllByTenantIdAndOrderId(entity.getTenantId(), entity.getId());
        List<OrderPaymentJpaEntity> paymentEntities = orderPaymentRepository.findAllByTenantIdAndOrderId(entity.getTenantId(), entity.getId());

        List<OrderItem> domainItems = itemEntities.stream().map(i -> new OrderItem(
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

        List<OrderPayment> domainPayments = paymentEntities.stream().map(p -> new OrderPayment(
                p.getId(),
                EcommercePaymentMethod.valueOf(p.getPaymentMethod()),
                PaymentGateway.valueOf(p.getGateway()),
                p.getGatewayTransactionId(),
                PaymentStatus.valueOf(p.getStatus()),
                p.getAmount(),
                p.getInstallments(),
                p.getPixQrCode(),
                p.getPixCopiaCola(),
                p.getBoletoBarcode(),
                p.getBoletoUrl(),
                p.getBoletoDueDate(),
                p.getCardBrand(),
                p.getCardLastFour(),
                p.getCreatedAt(),
                p.getUpdatedAt(),
                p.getPaidAt()
        )).toList();

        ShippingAddress address = null;
        if (entity.getShippingAddressStreet() != null || entity.getShippingAddressZipCode() != null) {
            address = new ShippingAddress(
                    entity.getShippingAddressStreet(),
                    entity.getShippingAddressNumber(),
                    entity.getShippingAddressComplement(),
                    entity.getShippingAddressNeighborhood(),
                    entity.getShippingAddressCity(),
                    entity.getShippingAddressState(),
                    entity.getShippingAddressZipCode()
            );
        }

        ShippingCarrier carrier = entity.getShippingCarrier() != null ? ShippingCarrier.valueOf(entity.getShippingCarrier()) : null;
        ShippingService service = entity.getShippingService() != null ? ShippingService.valueOf(entity.getShippingService()) : null;

        return new Order(
                entity.getId(),
                entity.getTenantId(),
                entity.getOrderNumber(),
                entity.getCustomerId(),
                entity.getCustomerName(),
                entity.getCustomerEmail(),
                entity.getCustomerDocument(),
                entity.getCustomerPhone(),
                entity.getOriginCompanyId(),
                OrderStatus.valueOf(entity.getStatus()),
                entity.getSubtotal(),
                entity.getDiscount(),
                entity.getShippingAmount(),
                entity.getTotalAmount(),
                carrier,
                service,
                entity.getTrackingCode(),
                entity.getTrackingUrl(),
                address,
                domainItems,
                domainPayments,
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPaidAt(),
                entity.getShippedAt(),
                entity.getDeliveredAt(),
                entity.getCanceledAt()
        );
    }
}
