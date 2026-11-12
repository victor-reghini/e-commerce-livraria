package com.livraria.ecommerce.domain.model.ecommerce;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Order {

    private UUID id;
    private UUID tenantId;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private String customerEmail;
    private String customerDocument;
    private String customerPhone;
    private UUID originCompanyId;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private ShippingCarrier shippingCarrier;
    private ShippingService shippingService;
    private String trackingCode;
    private String trackingUrl;
    private ShippingAddress shippingAddress;
    private List<OrderItem> items;
    private List<OrderPayment> payments;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant shippedAt;
    private Instant deliveredAt;
    private Instant canceledAt;

    public Order(UUID id, UUID tenantId, String orderNumber, UUID customerId,
                 String customerName, String customerEmail, String customerDocument, String customerPhone,
                 UUID originCompanyId, OrderStatus status, BigDecimal subtotal, BigDecimal discount,
                 BigDecimal shippingAmount, BigDecimal totalAmount, ShippingCarrier shippingCarrier,
                 ShippingService shippingService, String trackingCode, String trackingUrl,
                 ShippingAddress shippingAddress, List<OrderItem> items, List<OrderPayment> payments,
                 String notes, Instant createdAt, Instant updatedAt, Instant paidAt,
                 Instant shippedAt, Instant deliveredAt, Instant canceledAt) {
        if (tenantId == null) {
            throw new BusinessException("Tenant ID é obrigatório para o pedido.");
        }
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new BusinessException("Número do pedido é obrigatório.");
        }
        if (customerName == null || customerName.isBlank()) {
            throw new BusinessException("Nome do cliente é obrigatório.");
        }
        if (customerDocument == null || customerDocument.isBlank()) {
            throw new BusinessException("Documento (CPF/CNPJ) do cliente é obrigatório.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.orderNumber = orderNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerDocument = customerDocument.replaceAll("\\D", "");
        this.customerPhone = customerPhone;
        this.originCompanyId = originCompanyId;
        this.status = status != null ? status : OrderStatus.AGUARDANDO_PAGAMENTO;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.shippingAmount = shippingAmount != null ? shippingAmount : BigDecimal.ZERO;
        this.totalAmount = totalAmount != null ? totalAmount : subtotal.subtract(this.discount).add(this.shippingAmount);
        this.shippingCarrier = shippingCarrier;
        this.shippingService = shippingService;
        this.trackingCode = trackingCode;
        this.trackingUrl = trackingUrl;
        this.shippingAddress = shippingAddress;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.payments = payments != null ? new ArrayList<>(payments) : new ArrayList<>();
        this.notes = notes;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.paidAt = paidAt;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
        this.canceledAt = canceledAt;
    }

    public static Order create(UUID tenantId, String orderNumber, UUID customerId,
                               String customerName, String customerEmail, String customerDocument,
                               String customerPhone, UUID originCompanyId, BigDecimal discount,
                               BigDecimal shippingAmount, ShippingCarrier carrier, ShippingService service,
                               ShippingAddress address, List<OrderItem> items, String notes) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Pedido deve conter pelo menos um item.");
        }
        BigDecimal calculatedSubtotal = items.stream()
                .map(OrderItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disc = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal ship = shippingAmount != null ? shippingAmount : BigDecimal.ZERO;
        BigDecimal calculatedTotal = calculatedSubtotal.subtract(disc).add(ship);

        return new Order(UUID.randomUUID(), tenantId, orderNumber, customerId,
                customerName, customerEmail, customerDocument, customerPhone,
                originCompanyId, OrderStatus.AGUARDANDO_PAGAMENTO, calculatedSubtotal,
                disc, ship, calculatedTotal, carrier, service, null, null,
                address, items, new ArrayList<>(), notes, Instant.now(), Instant.now(),
                null, null, null, null);
    }

    public void addPayment(OrderPayment payment) {
        if (payment == null) {
            throw new BusinessException("Pagamento não pode ser nulo.");
        }
        this.payments.add(payment);
        this.updatedAt = Instant.now();
    }

    public void markAsPaid(String transactionId) {
        if (this.status == OrderStatus.CANCELADO) {
            throw new BusinessException("Não é possível aprovar pagamento de um pedido cancelado.");
        }
        this.status = OrderStatus.PAGO;
        this.paidAt = Instant.now();
        this.updatedAt = Instant.now();
        for (OrderPayment payment : payments) {
            if (payment.getStatus() == PaymentStatus.PENDENTE) {
                payment.markAsPaid(transactionId);
            }
        }
    }

    public void markAsInvoiced(LocalDate dueDate) {
        if (this.status == OrderStatus.CANCELADO) {
            throw new BusinessException("Não é possível faturar um pedido cancelado.");
        }
        this.status = OrderStatus.FATURADO_A_PRAZO;
        this.updatedAt = Instant.now();
    }

    public void startPreparation() {
        if (this.status != OrderStatus.PAGO && this.status != OrderStatus.FATURADO_A_PRAZO) {
            throw new BusinessException("Apenas pedidos pagos ou faturados a prazo podem entrar em separação.");
        }
        this.status = OrderStatus.EM_SEPARACAO;
        this.updatedAt = Instant.now();
    }

    public void ship(String trackingCode, String trackingUrl) {
        if (this.status != OrderStatus.PAGO && this.status != OrderStatus.FATURADO_A_PRAZO && this.status != OrderStatus.EM_SEPARACAO) {
            throw new BusinessException("Pedido deve estar pago ou faturado para ser despachado.");
        }
        this.status = OrderStatus.ENVIADO;
        this.trackingCode = trackingCode;
        this.trackingUrl = trackingUrl;
        this.shippedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void deliver() {
        if (this.status != OrderStatus.ENVIADO) {
            throw new BusinessException("Apenas pedidos enviados podem ser marcados como entregues.");
        }
        this.status = OrderStatus.ENTREGUE;
        this.deliveredAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == OrderStatus.ENTREGUE) {
            throw new BusinessException("Não é possível cancelar um pedido já entregue.");
        }
        this.status = OrderStatus.CANCELADO;
        this.canceledAt = Instant.now();
        this.updatedAt = Instant.now();
        if (reason != null && !reason.isBlank()) {
            this.notes = (this.notes != null ? this.notes + "\n" : "") + "Cancelamento: " + reason;
        }
        for (OrderPayment payment : payments) {
            if (payment.getStatus() == PaymentStatus.PENDENTE) {
                payment.cancel();
            }
        }
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getOrderNumber() { return orderNumber; }
    public UUID getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerDocument() { return customerDocument; }
    public String getCustomerPhone() { return customerPhone; }
    public UUID getOriginCompanyId() { return originCompanyId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public ShippingCarrier getShippingCarrier() { return shippingCarrier; }
    public ShippingService getShippingService() { return shippingService; }
    public String getTrackingCode() { return trackingCode; }
    public String getTrackingUrl() { return trackingUrl; }
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public List<OrderPayment> getPayments() { return Collections.unmodifiableList(payments); }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getShippedAt() { return shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getCanceledAt() { return canceledAt; }
}

