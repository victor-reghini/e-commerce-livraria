package com.livraria.ecommerce.application.dto.stock;

import com.livraria.ecommerce.domain.model.stock.AuditStatus;
import com.livraria.ecommerce.domain.model.stock.MovementType;
import com.livraria.ecommerce.domain.model.stock.TransferStatus;
import com.livraria.ecommerce.domain.model.stock.TransferType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EventStockDto {

    public record CreateTransferRequest(
            @NotNull UUID originCompanyId,
            @NotNull UUID destinationCompanyId,
            TransferType transferType,
            String notes,
            @NotNull List<TransferItemRequest> items
    ) {}

    public record TransferItemRequest(
            @NotNull UUID skuId,
            int quantity,
            BigDecimal unitPrice
    ) {}

    public record ReceiveTransferRequest(
            Map<UUID, Integer> receivedQuantities
    ) {}

    public record CreateAuditRequest(
            @NotNull UUID companyId,
            UUID transferId,
            String notes
    ) {}

    public record ScanBarcodeRequest(
            @NotNull String barcode,
            int quantity
    ) {}

    public record StockTransferResponse(
            UUID id,
            UUID tenantId,
            String transferNumber,
            UUID originCompanyId,
            String originCompanyName,
            UUID destinationCompanyId,
            String destinationCompanyName,
            TransferStatus status,
            TransferType transferType,
            String notes,
            List<StockTransferItemResponse> items,
            int totalItems,
            BigDecimal totalValue,
            Instant dispatchedAt,
            Instant receivedAt,
            UUID createdBy,
            UUID receivedBy,
            Instant createdAt
    ) {}

    public record StockTransferItemResponse(
            UUID id,
            UUID skuId,
            String skuCode,
            String title,
            int quantityRequested,
            int quantityDispatched,
            int quantityReceived,
            int divergence,
            BigDecimal unitCost,
            BigDecimal unitPrice
    ) {}

    public record BlindAuditResponse(
            UUID id,
            UUID tenantId,
            UUID companyId,
            String companyName,
            UUID transferId,
            String code,
            AuditStatus status,
            UUID auditorId,
            String notes,
            List<BlindAuditItemResponse> items,
            int totalScanned,
            int totalExpected,
            int totalDivergence,
            Instant createdAt,
            Instant completedAt
    ) {}

    public record BlindAuditItemResponse(
            UUID id,
            UUID skuId,
            String skuCode,
            String title,
            int quantityScanned,
            int quantityExpected,
            int divergence,
            Instant lastScannedAt
    ) {}

    public record StockMovementResponse(
            UUID id,
            UUID tenantId,
            UUID companyId,
            UUID skuId,
            MovementType movementType,
            int quantityChange,
            int balanceAfter,
            UUID referenceId,
            String reason,
            UUID createdBy,
            Instant createdAt
    ) {}

    public record RomaneioDocumentResponse(
            String transferNumber,
            String emitente,
            String destinatario,
            TransferType transferType,
            Instant dataEmissao,
            List<RomaneioItem> itens,
            int quantidadeTotalLivros,
            int pesoTotalGramas,
            BigDecimal valorTotalCarga,
            String observacoes
    ) {
        public record RomaneioItem(
                String skuCode,
                String isbn,
                String titulo,
                int quantidade,
                int pesoUnitarioGramas,
                BigDecimal precoUnitario,
                BigDecimal subtotal
        ) {}
    }
}
