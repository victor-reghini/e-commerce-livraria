package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.ecommerce.CorporateBudgetDto;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudget;
import com.livraria.ecommerce.domain.port.in.CorporateBudgetUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/budgets")
@Tag(name = "Vendas a Prazo e Orçamentos", description = "Gestão de orçamentos corporativos, faturamento a prazo e Trava de Inadimplência")
public class CorporateBudgetController {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final CorporateBudgetUseCase budgetUseCase;

    public CorporateBudgetController(CorporateBudgetUseCase budgetUseCase) {
        this.budgetUseCase = budgetUseCase;
    }

    private UUID getTenantId(String headerTenantId) {
        if (headerTenantId != null && !headerTenantId.isBlank()) {
            return UUID.fromString(headerTenantId);
        }
        return TenantContext.getTenantId().orElse(DEFAULT_TENANT_ID);
    }

    @PostMapping
    @Operation(summary = "Criar novo orçamento corporativo institucional")
    public ResponseEntity<CorporateBudgetDto.BudgetResponse> createBudget(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @Valid @RequestBody CorporateBudgetDto.CreateBudgetRequest request) {
        UUID tenantId = getTenantId(tenantIdHeader);
        CorporateBudget budget = budgetUseCase.createBudget(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(budget));
    }

    @GetMapping
    @Operation(summary = "Listar orçamentos corporativos do tenant")
    public ResponseEntity<List<CorporateBudgetDto.BudgetResponse>> listBudgets(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = getTenantId(tenantIdHeader);
        List<CorporateBudget> budgets = budgetUseCase.listBudgets(tenantId, page, size);
        return ResponseEntity.ok(budgets.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter espelho detalhado do orçamento corporativo")
    public ResponseEntity<CorporateBudgetDto.BudgetResponse> getBudget(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id) {
        UUID tenantId = getTenantId(tenantIdHeader);
        CorporateBudget budget = budgetUseCase.getBudget(tenantId, id);
        return ResponseEntity.ok(toResponse(budget));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submeter orçamento para aprovação formal")
    public ResponseEntity<CorporateBudgetDto.BudgetResponse> submitForApproval(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id) {
        UUID tenantId = getTenantId(tenantIdHeader);
        CorporateBudget budget = budgetUseCase.submitForApproval(tenantId, id);
        return ResponseEntity.ok(toResponse(budget));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Aprovar orçamento corporativo (Com Trava de Inadimplência automática)")
    public ResponseEntity<CorporateBudgetDto.BudgetResponse> approveBudget(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id) {
        UUID tenantId = getTenantId(tenantIdHeader);
        CorporateBudget budget = budgetUseCase.approveBudget(tenantId, id);
        return ResponseEntity.ok(toResponse(budget));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Rejeitar orçamento corporativo com justificativa")
    public ResponseEntity<CorporateBudgetDto.BudgetResponse> rejectBudget(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id,
            @Valid @RequestBody CorporateBudgetDto.RejectBudgetRequest request) {
        UUID tenantId = getTenantId(tenantIdHeader);
        CorporateBudget budget = budgetUseCase.rejectBudget(tenantId, id, request.reason());
        return ResponseEntity.ok(toResponse(budget));
    }

    @PostMapping("/{id}/invoice")
    @Operation(summary = "Faturar orçamento aprovado: cria pedido a prazo com boleto de 30 dias e debita estoque")
    public ResponseEntity<CorporateBudgetDto.BudgetResponse> invoiceBudget(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @PathVariable UUID id) {
        UUID tenantId = getTenantId(tenantIdHeader);
        budgetUseCase.invoiceBudget(tenantId, id);
        CorporateBudget updatedBudget = budgetUseCase.getBudget(tenantId, id);
        return ResponseEntity.ok(toResponse(updatedBudget));
    }

    private CorporateBudgetDto.BudgetResponse toResponse(CorporateBudget budget) {
        List<CorporateBudgetDto.BudgetItemResponse> items = budget.getItems().stream().map(i ->
                new CorporateBudgetDto.BudgetItemResponse(
                        i.getId(),
                        i.getSkuId(),
                        i.getSkuCode(),
                        i.getProductTitle(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getDiscount(),
                        i.getTotal()
                )).toList();

        return new CorporateBudgetDto.BudgetResponse(
                budget.getId(),
                budget.getBudgetNumber(),
                budget.getCustomerId(),
                budget.getCorporateName(),
                budget.getTradeName(),
                budget.getCnpj(),
                budget.getStateRegistration(),
                budget.getContactName(),
                budget.getContactEmail(),
                budget.getContactPhone(),
                budget.getStatus(),
                budget.getSubtotal(),
                budget.getDiscount(),
                budget.getShippingAmount(),
                budget.getTotalAmount(),
                budget.getPaymentTermsDays(),
                budget.getDueDate(),
                budget.getNotes(),
                budget.getRejectionReason(),
                budget.getInvoicedOrderId(),
                items,
                budget.getCreatedAt(),
                budget.getApprovedAt()
        );
    }
}
