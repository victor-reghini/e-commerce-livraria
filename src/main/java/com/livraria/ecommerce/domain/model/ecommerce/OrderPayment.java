package com.livraria.ecommerce.domain.model.ecommerce;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class OrderPayment {

    private UUID id;
    private EcommercePaymentMethod paymentMethod;
    private PaymentGateway gateway;
    private String gatewayTransactionId;
    private PaymentStatus status;
    private BigDecimal amount;
    private int installments;
    private String pixQrCode;
    private String pixCopiaCola;
    private String boletoBarcode;
    private String boletoUrl;
    private LocalDate boletoDueDate;
    private String cardBrand;
    private String cardLastFour;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;

    public OrderPayment(UUID id, EcommercePaymentMethod paymentMethod, PaymentGateway gateway,
                        String gatewayTransactionId, PaymentStatus status, BigDecimal amount,
                        int installments, String pixQrCode, String pixCopiaCola,
                        String boletoBarcode, String boletoUrl, LocalDate boletoDueDate,
                        String cardBrand, String cardLastFour,
                        Instant createdAt, Instant updatedAt, Instant paidAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.paymentMethod = paymentMethod;
        this.gateway = gateway != null ? gateway : PaymentGateway.PAGARME;
        this.gatewayTransactionId = gatewayTransactionId;
        this.status = status != null ? status : PaymentStatus.PENDENTE;
        this.amount = amount;
        this.installments = installments > 0 ? installments : 1;
        this.pixQrCode = pixQrCode;
        this.pixCopiaCola = pixCopiaCola;
        this.boletoBarcode = boletoBarcode;
        this.boletoUrl = boletoUrl;
        this.boletoDueDate = boletoDueDate;
        this.cardBrand = cardBrand;
        this.cardLastFour = cardLastFour;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.paidAt = paidAt;
    }

    public void markAsPaid(String transactionId) {
        this.status = PaymentStatus.PAGO;
        if (transactionId != null && !transactionId.isBlank()) {
            this.gatewayTransactionId = transactionId;
        }
        this.paidAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markAsFailed() {
        this.status = PaymentStatus.RECUSADO;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELADO;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public EcommercePaymentMethod getPaymentMethod() { return paymentMethod; }
    public PaymentGateway getGateway() { return gateway; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public int getInstallments() { return installments; }
    public String getPixQrCode() { return pixQrCode; }
    public String getPixCopiaCola() { return pixCopiaCola; }
    public String getBoletoBarcode() { return boletoBarcode; }
    public String getBoletoUrl() { return boletoUrl; }
    public LocalDate getBoletoDueDate() { return boletoDueDate; }
    public String getCardBrand() { return cardBrand; }
    public String getCardLastFour() { return cardLastFour; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getPaidAt() { return paidAt; }
}
