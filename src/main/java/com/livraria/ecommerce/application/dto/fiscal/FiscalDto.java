package com.livraria.ecommerce.application.dto.fiscal;

import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import com.livraria.ecommerce.domain.model.fiscal.FiscalModel;
import com.livraria.ecommerce.domain.model.fiscal.FiscalStatus;
import com.livraria.ecommerce.domain.model.fiscal.SefazStatusResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FiscalDto {

    public record EmitNfeRequest(
            Integer series,
            String natureOfOperation
    ) {}

    public record EmitNfceRequest(
            Integer series
    ) {}

    public record CancelInvoiceRequest(
            @NotBlank(message = "A justificativa de cancelamento é obrigatória")
            @Size(min = 15, max = 255, message = "A justificativa deve conter entre 15 e 255 caracteres")
            String reason
    ) {}

    public record FiscalInvoiceResponse(
            UUID id,
            UUID tenantId,
            UUID companyId,
            UUID orderId,
            UUID saleId,
            FiscalModel model,
            String modelDescription,
            int series,
            long invoiceNumber,
            String accessKey,
            FiscalStatus status,
            String statusDescription,
            String natureOfOperation,
            BigDecimal totalAmount,
            BigDecimal totalDiscount,
            BigDecimal totalTaxIcms,
            BigDecimal totalTaxPis,
            BigDecimal totalTaxCofins,
            String protocolNumber,
            String danfeUrl,
            String rejectionReason,
            String cancellationReason,
            Instant createdAt,
            Instant authorizedAt,
            Instant canceledAt
    ) {
        public static FiscalInvoiceResponse fromDomain(FiscalInvoice invoice) {
            return new FiscalInvoiceResponse(
                    invoice.getId(),
                    invoice.getTenantId(),
                    invoice.getCompanyId(),
                    invoice.getOrderId(),
                    invoice.getSaleId(),
                    invoice.getModel(),
                    invoice.getModel() != null ? invoice.getModel().getDescription() : null,
                    invoice.getSeries(),
                    invoice.getInvoiceNumber(),
                    invoice.getAccessKey(),
                    invoice.getStatus(),
                    invoice.getStatus() != null ? invoice.getStatus().getDescription() : null,
                    invoice.getNatureOfOperation(),
                    invoice.getTotalAmount(),
                    invoice.getTotalDiscount(),
                    invoice.getTotalTaxIcms(),
                    invoice.getTotalTaxPis(),
                    invoice.getTotalTaxCofins(),
                    invoice.getProtocolNumber(),
                    invoice.getDanfeUrl(),
                    invoice.getRejectionReason(),
                    invoice.getCancellationReason(),
                    invoice.getCreatedAt(),
                    invoice.getAuthorizedAt(),
                    invoice.getCanceledAt()
            );
        }
    }

    public record SefazStatusResponse(
            boolean operational,
            int statusCode,
            String statusMessage,
            String uf,
            String environment,
            long averageResponseTimeMs,
            Instant checkedAt
    ) {
        public static SefazStatusResponse fromDomain(SefazStatusResult result) {
            return new SefazStatusResponse(
                    result.operational(),
                    result.statusCode(),
                    result.statusMessage(),
                    result.uf(),
                    result.environment(),
                    result.averageResponseTimeMs(),
                    result.checkedAt()
            );
        }
    }
}

