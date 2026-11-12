package com.livraria.ecommerce.application.dto.ecommerce;

import com.livraria.ecommerce.domain.model.ecommerce.BudgetStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CorporateBudgetDto {

    public record BudgetItemRequest(
            @NotNull(message = "SKU ID é obrigatório")
            UUID skuId,

            @Min(value = 1, message = "Quantidade mínima é 1")
            int quantity,

            BigDecimal customUnitPrice,
            BigDecimal discount
    ) {}

    public record CreateBudgetRequest(
            UUID customerId,

            @NotBlank(message = "Razão Social é obrigatória")
            String corporateName,

            String tradeName,

            @NotBlank(message = "CNPJ é obrigatório")
            String cnpj,

            String stateRegistration,

            @NotBlank(message = "Nome do contato é obrigatório")
            String contactName,

            @NotBlank(message = "E-mail de contato é obrigatório")
            String contactEmail,

            String contactPhone,

            @Min(value = 1, message = "Prazo de faturamento deve ser no mínimo 1 dia")
            int paymentTermsDays,

            BigDecimal discount,
            BigDecimal shippingAmount,

            @NotEmpty(message = "O orçamento deve conter itens")
            @Valid
            List<BudgetItemRequest> items,

            String notes
    ) {}

    public record BudgetItemResponse(
            UUID id,
            UUID skuId,
            String skuCode,
            String productTitle,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal discount,
            BigDecimal total
    ) {}

    public record BudgetResponse(
            UUID id,
            String budgetNumber,
            UUID customerId,
            String corporateName,
            String tradeName,
            String cnpj,
            String stateRegistration,
            String contactName,
            String contactEmail,
            String contactPhone,
            BudgetStatus status,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal shippingAmount,
            BigDecimal totalAmount,
            int paymentTermsDays,
            LocalDate dueDate,
            String notes,
            String rejectionReason,
            UUID invoicedOrderId,
            List<BudgetItemResponse> items,
            Instant createdAt,
            Instant approvedAt
    ) {}

    public record RejectBudgetRequest(
            @NotBlank(message = "Motivo da rejeição é obrigatório")
            String reason
    ) {}
}

