package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.stock.EventStockDto;
import com.livraria.ecommerce.domain.model.stock.BlindAudit;
import com.livraria.ecommerce.domain.model.stock.BlindAuditItem;
import com.livraria.ecommerce.domain.model.stock.EventDashboard;
import com.livraria.ecommerce.domain.model.stock.StockMovement;
import com.livraria.ecommerce.domain.model.stock.StockTransfer;
import com.livraria.ecommerce.domain.model.stock.StockTransferItem;
import com.livraria.ecommerce.domain.port.in.EventStockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Múltiplos Estoques, Transferências e Feiras", description = "Endpoints para gestão de remessas para eventos, romaneios, conferência cega e auditoria de estoque")
public class EventStockController {

    private final EventStockUseCase eventStockUseCase;

    public EventStockController(EventStockUseCase eventStockUseCase) {
        this.eventStockUseCase = eventStockUseCase;
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.livraria.ecommerce.infrastructure.security.UserPrincipal principal) {
            return principal.getId();
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000002");
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyAuthority('STOCK_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Criar nova transferência/remessa de estoque para evento ou filial")
    public ResponseEntity<EventStockDto.StockTransferResponse> createTransfer(
            @Valid @RequestBody EventStockDto.CreateTransferRequest request,
            Authentication authentication) {
        StockTransfer transfer = eventStockUseCase.createTransfer(request, getUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(toTransferResponse(transfer));
    }

    @GetMapping("/transfers")
    @PreAuthorize("hasAnyAuthority('STOCK_READ', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Listar transferências de estoque (opcional por filial/feira)")
    public ResponseEntity<List<EventStockDto.StockTransferResponse>> listTransfers(
            @RequestParam(required = false) UUID companyId) {
        List<StockTransfer> list = companyId != null
                ? eventStockUseCase.listTransfersByCompany(companyId)
                : eventStockUseCase.listAllTransfers();
        return ResponseEntity.ok(list.stream().map(this::toTransferResponse).toList());
    }

    @GetMapping("/transfers/{id}")
    @PreAuthorize("hasAnyAuthority('STOCK_READ', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Obter detalhes de uma transferência de estoque por ID")
    public ResponseEntity<EventStockDto.StockTransferResponse> getTransferById(@PathVariable UUID id) {
        StockTransfer transfer = eventStockUseCase.getTransferById(id);
        return ResponseEntity.ok(toTransferResponse(transfer));
    }

    @PostMapping("/transfers/{id}/dispatch")
    @PreAuthorize("hasAnyAuthority('STOCK_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Despachar transferência de estoque (coloca em trânsito)")
    public ResponseEntity<EventStockDto.StockTransferResponse> dispatchTransfer(
            @PathVariable UUID id,
            Authentication authentication) {
        StockTransfer transfer = eventStockUseCase.dispatchTransfer(id, getUserId(authentication));
        return ResponseEntity.ok(toTransferResponse(transfer));
    }

    @PostMapping("/transfers/{id}/receive")
    @PreAuthorize("hasAnyAuthority('STOCK_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Confirmar recebimento de transferência com detecção de divergência")
    public ResponseEntity<EventStockDto.StockTransferResponse> receiveTransfer(
            @PathVariable UUID id,
            @RequestBody(required = false) EventStockDto.ReceiveTransferRequest request,
            Authentication authentication) {
        StockTransfer transfer = eventStockUseCase.receiveTransfer(id, request, getUserId(authentication));
        return ResponseEntity.ok(toTransferResponse(transfer));
    }

    @GetMapping("/transfers/{id}/romaneio")
    @PreAuthorize("hasAnyAuthority('STOCK_READ', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Gerar romaneio formal de carga/remessa para evento")
    public ResponseEntity<EventStockDto.RomaneioDocumentResponse> getRomaneio(@PathVariable UUID id) {
        EventStockDto.RomaneioDocumentResponse romaneio = eventStockUseCase.generateRomaneio(id);
        return ResponseEntity.ok(romaneio);
    }

    @PostMapping("/audits")
    @PreAuthorize("hasAnyAuthority('STOCK_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Criar nova conferência cega (bipagem de retorno de evento)")
    public ResponseEntity<EventStockDto.BlindAuditResponse> createAudit(
            @Valid @RequestBody EventStockDto.CreateAuditRequest request,
            Authentication authentication) {
        BlindAudit audit = eventStockUseCase.createBlindAudit(request, getUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuditResponse(audit));
    }

    @GetMapping("/audits")
    @PreAuthorize("hasAnyAuthority('STOCK_READ', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Listar conferências cegas de uma filial/feira")
    public ResponseEntity<List<EventStockDto.BlindAuditResponse>> listAudits(@RequestParam UUID companyId) {
        List<BlindAudit> list = eventStockUseCase.listBlindAuditsByCompany(companyId);
        return ResponseEntity.ok(list.stream().map(this::toAuditResponse).toList());
    }

    @GetMapping("/audits/{id}")
    @PreAuthorize("hasAnyAuthority('STOCK_READ', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Obter detalhes de uma conferência cega por ID")
    public ResponseEntity<EventStockDto.BlindAuditResponse> getAuditById(@PathVariable UUID id) {
        BlindAudit audit = eventStockUseCase.getBlindAuditById(id);
        return ResponseEntity.ok(toAuditResponse(audit));
    }

    @PostMapping("/audits/{id}/scan")
    @PreAuthorize("hasAnyAuthority('STOCK_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Bipar código de barras ou ISBN em conferência cega")
    public ResponseEntity<EventStockDto.BlindAuditResponse> scanBarcode(
            @PathVariable UUID id,
            @Valid @RequestBody EventStockDto.ScanBarcodeRequest request) {
        BlindAudit audit = eventStockUseCase.scanBarcode(id, request.barcode(), request.quantity());
        return ResponseEntity.ok(toAuditResponse(audit));
    }

    @PostMapping("/audits/{id}/finalize")
    @PreAuthorize("hasAnyAuthority('STOCK_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Finalizar bipagem de conferência cega")
    public ResponseEntity<EventStockDto.BlindAuditResponse> finalizeAudit(@PathVariable UUID id) {
        BlindAudit audit = eventStockUseCase.finalizeAudit(id);
        return ResponseEntity.ok(toAuditResponse(audit));
    }

    @PostMapping("/audits/{id}/reconcile")
    @PreAuthorize("hasAnyAuthority('STOCK_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Conciliar conferência cega ajustando saldo real de estoque")
    public ResponseEntity<EventStockDto.BlindAuditResponse> reconcileAudit(
            @PathVariable UUID id,
            Authentication authentication) {
        BlindAudit audit = eventStockUseCase.reconcileAudit(id, getUserId(authentication));
        return ResponseEntity.ok(toAuditResponse(audit));
    }

    @GetMapping("/stock/movements")
    @PreAuthorize("hasAnyAuthority('STOCK_READ', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Listar histórico imutável de movimentações de estoque (ledger)")
    public ResponseEntity<List<EventStockDto.StockMovementResponse>> listMovements(
            @RequestParam UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<StockMovement> movements = eventStockUseCase.listMovementsByCompany(companyId, page, size);
        List<EventStockDto.StockMovementResponse> res = movements.stream().map(m -> new EventStockDto.StockMovementResponse(
                m.getId(), m.getTenantId(), m.getCompanyId(), m.getSkuId(),
                m.getMovementType(), m.getQuantityChange(), m.getBalanceAfter(),
                m.getReferenceId(), m.getReason(), m.getCreatedBy(), m.getCreatedAt()
        )).toList();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/events/{companyId}/dashboard")
    @PreAuthorize("hasAnyAuthority('STOCK_READ', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Obter métricas e dashboard do evento/feira em tempo real")
    public ResponseEntity<EventDashboard> getEventDashboard(@PathVariable UUID companyId) {
        EventDashboard dashboard = eventStockUseCase.getEventDashboard(companyId);
        return ResponseEntity.ok(dashboard);
    }

    private EventStockDto.StockTransferResponse toTransferResponse(StockTransfer t) {
        List<EventStockDto.StockTransferItemResponse> items = t.getItems().stream().map(i ->
                new EventStockDto.StockTransferItemResponse(
                        i.getId(), i.getSkuId(), i.getSkuCode(), i.getTitle(),
                        i.getQuantityRequested(), i.getQuantityDispatched(), i.getQuantityReceived(),
                        i.getDivergence(), i.getUnitCost(), i.getUnitPrice()
                )
        ).toList();

        return new EventStockDto.StockTransferResponse(
                t.getId(), t.getTenantId(), t.getTransferNumber(),
                t.getOriginCompanyId(), t.getOriginCompanyName(),
                t.getDestinationCompanyId(), t.getDestinationCompanyName(),
                t.getStatus(), t.getTransferType(), t.getNotes(),
                items, t.calculateTotalItems(), t.calculateTotalValue(),
                t.getDispatchedAt(), t.getReceivedAt(),
                t.getCreatedBy(), t.getReceivedBy(), t.getCreatedAt()
        );
    }

    private EventStockDto.BlindAuditResponse toAuditResponse(BlindAudit a) {
        List<EventStockDto.BlindAuditItemResponse> items = a.getItems().stream().map(i ->
                new EventStockDto.BlindAuditItemResponse(
                        i.getId(), i.getSkuId(), i.getSkuCode(), i.getTitle(),
                        i.getQuantityScanned(), i.getQuantityExpected(),
                        i.getDivergence(), i.getLastScannedAt()
                )
        ).toList();

        return new EventStockDto.BlindAuditResponse(
                a.getId(), a.getTenantId(), a.getCompanyId(), a.getCompanyName(),
                a.getTransferId(), a.getCode(), a.getStatus(),
                a.getAuditorId(), a.getNotes(), items,
                a.getTotalScanned(), a.getTotalExpected(), a.getTotalDivergence(),
                a.getCreatedAt(), a.getCompletedAt()
        );
    }
}

