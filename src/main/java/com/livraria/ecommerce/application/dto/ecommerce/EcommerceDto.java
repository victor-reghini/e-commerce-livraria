package com.livraria.ecommerce.application.dto.ecommerce;

import com.livraria.ecommerce.domain.model.ecommerce.EcommercePaymentMethod;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentStatus;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingCarrier;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class EcommerceDto {

    public record AddCartItemRequest(
            @NotNull(message = "SKU ID é obrigatório")
            UUID skuId,

            @Min(value = 1, message = "Quantidade deve ser no mínimo 1")
            int quantity
    ) {}

    public record UpdateCartItemRequest(
            @Min(value = 0, message = "Quantidade não pode ser negativa")
            int quantity
    ) {}

    public record CartItemResponse(
            UUID id,
            UUID skuId,
            String skuCode,
            String productTitle,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal total
    ) {}

    public record CartResponse(
            UUID id,
            String sessionId,
            UUID customerId,
            List<CartItemResponse> items,
            BigDecimal subtotal,
            int totalItems,
            Instant updatedAt
    ) {}

    public record CalculateShippingRequest(
            @NotBlank(message = "CEP de destino é obrigatório")
            String destinationZip,

            String sessionId,
            UUID customerId
    ) {}

    public record ShippingQuoteResponse(
            ShippingCarrier carrier,
            ShippingService service,
            String name,
            BigDecimal price,
            int deliveryDays,
            boolean available,
            String error
    ) {}

    public record ShippingAddressDto(
            @NotBlank(message = "Logradouro é obrigatório")
            String street,

            @NotBlank(message = "Número é obrigatório")
            String number,

            String complement,

            @NotBlank(message = "Bairro é obrigatório")
            String neighborhood,

            @NotBlank(message = "Cidade é obrigatória")
            String city,

            @NotBlank(message = "Estado é obrigatório")
            String state,

            @NotBlank(message = "CEP é obrigatório")
            String zipCode
    ) {}

    public record CheckoutRequest(
            String sessionId,
            UUID customerId,

            @NotBlank(message = "Nome do cliente é obrigatório")
            String customerName,

            @NotBlank(message = "E-mail do cliente é obrigatório")
            String customerEmail,

            @NotBlank(message = "CPF/CNPJ do cliente é obrigatório")
            String customerDocument,

            String customerPhone,

            @NotNull(message = "Endereço de entrega é obrigatório")
            ShippingAddressDto shippingAddress,

            @NotNull(message = "Transportadora é obrigatória")
            ShippingCarrier shippingCarrier,

            @NotNull(message = "Serviço de frete é obrigatório")
            ShippingService shippingService,

            @NotNull(message = "Método de pagamento é obrigatório")
            EcommercePaymentMethod paymentMethod,

            Integer installments,
            String cardToken,
            String notes
    ) {}

    public record OrderItemResponse(
            UUID id,
            UUID skuId,
            String skuCode,
            String productTitle,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal discount,
            BigDecimal total,
            String ncm,
            String cfop
    ) {}

    public record OrderPaymentResponse(
            UUID id,
            EcommercePaymentMethod paymentMethod,
            PaymentStatus status,
            BigDecimal amount,
            int installments,
            String pixQrCode,
            String pixCopiaCola,
            String boletoBarcode,
            String boletoUrl,
            LocalDate boletoDueDate,
            String cardBrand,
            String cardLastFour
    ) {}

    public record OrderResponse(
            UUID id,
            String orderNumber,
            String customerName,
            String customerEmail,
            String customerDocument,
            String customerPhone,
            OrderStatus status,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shippingAmount,
            BigDecimal totalAmount,
            ShippingCarrier shippingCarrier,
            ShippingService shippingService,
            String trackingCode,
            String trackingUrl,
            String shippingAddressFormatted,
            List<OrderItemResponse> items,
            List<OrderPaymentResponse> payments,
            Instant createdAt,
            Instant paidAt,
            Instant shippedAt,
            Instant deliveredAt,
            Instant canceledAt
    ) {}

    public record ShipOrderRequest(
            @NotBlank(message = "Código de rastreio é obrigatório")
            String trackingCode,
            String trackingUrl
    ) {}

    public record CancelOrderRequest(
            @NotBlank(message = "Motivo do cancelamento é obrigatório")
            String reason
    ) {}

    public record PagarmeWebhookPayload(
            String id,
            String type,
            Data data
    ) {
        public record Data(
                String id,
                String code,
                BigDecimal amount,
                String status,
                List<Charge> charges
        ) {}

        public record Charge(
                String id,
                String code,
                String status,
                BigDecimal amount,
                LastTransaction last_transaction
        ) {}

        public record LastTransaction(
                String id,
                String transaction_type,
                String status,
                String qr_code,
                String qr_code_url,
                String url,
                String barcode
        ) {}
    }
}

