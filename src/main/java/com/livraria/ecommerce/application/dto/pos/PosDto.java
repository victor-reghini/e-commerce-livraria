package com.livraria.ecommerce.application.dto.pos;

import com.livraria.ecommerce.domain.model.pos.CashMovementType;
import com.livraria.ecommerce.domain.model.pos.NfceStatus;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import com.livraria.ecommerce.domain.model.pos.ShiftStatus;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PosDto {

    public record OpenShiftRequest(
            @NotNull UUID companyId,
            BigDecimal openingBalance
    ) {}

    public record CashMovementRequest(
            @NotNull BigDecimal amount,
            String reason
    ) {}

    public record CloseShiftRequest(
            @NotNull BigDecimal countedCash,
            String notes
    ) {}

    public record CreateSaleRequest(
            @NotNull UUID companyId,
            UUID shiftId,
            UUID clientUuid, // Idempotency key para vendas offline
            String customerName,
            String customerCpfCnpj,
            BigDecimal discount,
            @NotNull List<SaleItemRequest> items,
            @NotNull List<SalePaymentRequest> payments
    ) {}

    public record SaleItemRequest(
            @NotNull UUID skuId,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal discount
    ) {}

    public record SalePaymentRequest(
            @NotNull PaymentMethod paymentMethod,
            @NotNull BigDecimal amount
    ) {}

    public record CashShiftResponse(
            UUID id,
            UUID tenantId,
            UUID companyId,
            UUID cashierId,
            String cashierName,
            String shiftNumber,
            ShiftStatus status,
            BigDecimal openingBalance,
            BigDecimal expectedBalance,
            BigDecimal actualBalance,
            BigDecimal divergence,
            String notes,
            List<CashMovementResponse> movements,
            BigDecimal totalDinheiro,
            BigDecimal totalPix,
            BigDecimal totalCartaoDebito,
            BigDecimal totalCartaoCredito,
            Instant openedAt,
            Instant closedAt
    ) {}

    public record CashMovementResponse(
            UUID id,
            UUID shiftId,
            CashMovementType movementType,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String reason,
            UUID operatorId,
            Instant createdAt
    ) {}

    public record PosSaleResponse(
            UUID id,
            UUID tenantId,
            UUID companyId,
            UUID shiftId,
            String saleNumber,
            UUID clientUuid,
            String customerName,
            String customerCpfCnpj,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            BigDecimal totalPaid,
            BigDecimal changeAmount,
            SaleStatus status,
            String nfceKey,
            NfceStatus nfceStatus,
            List<PosSaleItemResponse> items,
            List<PosSalePaymentResponse> payments,
            UUID createdBy,
            Instant createdAt
    ) {}

    public record PosSaleItemResponse(
            UUID id,
            UUID skuId,
            String skuCode,
            String title,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal discount,
            BigDecimal subtotal
    ) {}

    public record PosSalePaymentResponse(
            UUID id,
            PaymentMethod paymentMethod,
            BigDecimal amount
    ) {}

    public record OfflineCatalogItem(
            UUID skuId,
            UUID productId,
            String skuCode,
            String barcode,
            String isbn,
            String title,
            BigDecimal price,
            BigDecimal promotionalPrice,
            int physicalStock,
            int availableStock
    ) {}

    public record OfflineCatalogSyncResponse(
            UUID companyId,
            Instant synchronizedAt,
            int totalItems,
            List<OfflineCatalogItem> items
    ) {}

    public record OfflineBatchSyncResult(
            int totalReceived,
            int successfullyProcessed,
            int alreadyProcessedDuplicates,
            int failures,
            List<PosSaleResponse> processedSales
    ) {}

    public record ThermalReceiptResponse(
            String saleNumber,
            String companyName,
            String companyCnpj,
            String address,
            Instant emissionDate,
            List<ReceiptItem> items,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal total,
            List<ReceiptPayment> payments,
            BigDecimal changeAmount,
            String nfceKey,
            String qrCodeUrl,
            String operatorName
    ) {
        public record ReceiptItem(
                String skuCode,
                String title,
                int quantity,
                BigDecimal unitPrice,
                BigDecimal subtotal
        ) {}

        public record ReceiptPayment(
                PaymentMethod paymentMethod,
                BigDecimal amount
        ) {}
    }
}
