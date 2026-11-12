package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.pos.PosDto;
import com.livraria.ecommerce.domain.model.pos.CashMovement;
import com.livraria.ecommerce.domain.model.pos.CashShift;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.PosSalePayment;
import com.livraria.ecommerce.domain.port.in.PosUseCase;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pos")
@Tag(name = "PDV Mobile e Venda Balcão / Feira", description = "Endpoints para controle de turnos de caixa, suprimento, sangria, vendas balcão, offline sync e emissão de NFC-e")
public class PosController {

    private final PosUseCase posUseCase;

    public PosController(PosUseCase posUseCase) {
        this.posUseCase = posUseCase;
    }

    private UUID getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.livraria.ecommerce.infrastructure.security.UserPrincipal principal) {
            return principal.getId();
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000002");
    }

    @PostMapping("/shifts/open")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Abrir novo turno de caixa com saldo inicial")
    public ResponseEntity<PosDto.CashShiftResponse> openShift(
            @Valid @RequestBody PosDto.OpenShiftRequest request,
            Authentication authentication) {
        CashShift shift = posUseCase.openShift(request.companyId(), request.openingBalance(), getUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(toShiftResponse(shift));
    }

    @PostMapping("/shifts/{id}/supply")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Realizar suprimento de dinheiro no caixa (reforço de troco)")
    public ResponseEntity<PosDto.CashShiftResponse> supplyCash(
            @PathVariable UUID id,
            @Valid @RequestBody PosDto.CashMovementRequest request,
            Authentication authentication) {
        CashShift shift = posUseCase.supplyCash(id, request.amount(), request.reason(), getUserId(authentication));
        return ResponseEntity.ok(toShiftResponse(shift));
    }

    @PostMapping("/shifts/{id}/bleed")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Realizar sangria de dinheiro no caixa (retirada para cofre)")
    public ResponseEntity<PosDto.CashShiftResponse> bleedCash(
            @PathVariable UUID id,
            @Valid @RequestBody PosDto.CashMovementRequest request,
            Authentication authentication) {
        CashShift shift = posUseCase.bleedCash(id, request.amount(), request.reason(), getUserId(authentication));
        return ResponseEntity.ok(toShiftResponse(shift));
    }

    @PostMapping("/shifts/{id}/close")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Fechamento cego de caixa com apuração de divergência / quebra")
    public ResponseEntity<PosDto.CashShiftResponse> closeShift(
            @PathVariable UUID id,
            @Valid @RequestBody PosDto.CloseShiftRequest request,
            Authentication authentication) {
        CashShift shift = posUseCase.closeShift(id, request.countedCash(), request.notes(), getUserId(authentication));
        return ResponseEntity.ok(toShiftResponse(shift));
    }

    @GetMapping("/shifts/{id}")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Obter detalhes de um turno de caixa por ID")
    public ResponseEntity<PosDto.CashShiftResponse> getShiftById(@PathVariable UUID id) {
        CashShift shift = posUseCase.getShiftById(id);
        return ResponseEntity.ok(toShiftResponse(shift));
    }

    @GetMapping("/shifts/current")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Obter turno de caixa atualmente aberto para o operador ativo")
    public ResponseEntity<PosDto.CashShiftResponse> getCurrentShift(
            @RequestParam UUID companyId,
            Authentication authentication) {
        return posUseCase.getCurrentOpenShift(companyId, getUserId(authentication))
                .map(s -> ResponseEntity.ok(toShiftResponse(s)))
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/shifts")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Listar turnos de caixa da filial")
    public ResponseEntity<List<PosDto.CashShiftResponse>> listShifts(
            @RequestParam UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<CashShift> shifts = posUseCase.listShifts(companyId, page, size);
        return ResponseEntity.ok(shifts.stream().map(this::toShiftResponse).toList());
    }

    @PostMapping("/sales")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Realizar venda no balcão/feira com baixa imediata de estoque e NFC-e")
    public ResponseEntity<PosDto.PosSaleResponse> createSale(
            @Valid @RequestBody PosDto.CreateSaleRequest request,
            Authentication authentication) {
        PosSale sale = posUseCase.createSale(request, getUserId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(toSaleResponse(sale));
    }

    @GetMapping("/sales/{id}")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Obter detalhes de uma venda do PDV por ID")
    public ResponseEntity<PosDto.PosSaleResponse> getSaleById(@PathVariable UUID id) {
        PosSale sale = posUseCase.getSaleById(id);
        return ResponseEntity.ok(toSaleResponse(sale));
    }

    @PostMapping("/sales/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Cancelar venda no PDV e estornar estoque físico")
    public ResponseEntity<PosDto.PosSaleResponse> cancelSale(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        PosSale sale = posUseCase.cancelSale(id, reason, getUserId(authentication));
        return ResponseEntity.ok(toSaleResponse(sale));
    }

    @GetMapping("/sales")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Listar vendas da filial ou do turno de caixa")
    public ResponseEntity<List<PosDto.PosSaleResponse>> listSales(
            @RequestParam(required = false) UUID shiftId,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<PosSale> sales;
        if (shiftId != null) {
            sales = posUseCase.listSalesByShift(shiftId);
        } else if (companyId != null) {
            sales = posUseCase.listSalesByCompany(companyId, page, size);
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(sales.stream().map(this::toSaleResponse).toList());
    }

    @GetMapping("/sales/{id}/receipt")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Obter comprovante/cupom formatado para impressora térmica ESC/POS ou WhatsApp")
    public ResponseEntity<PosDto.ThermalReceiptResponse> getThermalReceipt(@PathVariable UUID id) {
        PosDto.ThermalReceiptResponse receipt = posUseCase.getThermalReceipt(id);
        return ResponseEntity.ok(receipt);
    }

    @GetMapping("/sync/catalog")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Sincronização delta de catálogo e estoques para PDV offline")
    public ResponseEntity<PosDto.OfflineCatalogSyncResponse> syncCatalog(
            @RequestParam UUID companyId,
            @RequestParam(required = false) Instant updatedSince) {
        PosDto.OfflineCatalogSyncResponse sync = posUseCase.syncCatalog(companyId, updatedSince);
        return ResponseEntity.ok(sync);
    }

    @PostMapping("/sync/offline-sales")
    @PreAuthorize("hasAnyAuthority('PDV_SELL', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Ingestão resiliente em lote de vendas offline com chave de idempotência")
    public ResponseEntity<PosDto.OfflineBatchSyncResult> syncOfflineSales(
            @RequestBody List<PosDto.CreateSaleRequest> sales,
            Authentication authentication) {
        PosDto.OfflineBatchSyncResult result = posUseCase.syncOfflineSales(sales, getUserId(authentication));
        return ResponseEntity.ok(result);
    }

    private PosDto.CashShiftResponse toShiftResponse(CashShift s) {
        List<PosDto.CashMovementResponse> movements = s.getMovements().stream().map(m ->
                new PosDto.CashMovementResponse(
                        m.getId(), m.getShiftId(), m.getMovementType(), m.getAmount(),
                        m.getPaymentMethod(), m.getReason(), m.getOperatorId(), m.getCreatedAt()
                )
        ).toList();

        return new PosDto.CashShiftResponse(
                s.getId(), s.getTenantId(), s.getCompanyId(), s.getCashierId(), s.getCashierName(),
                s.getShiftNumber(), s.getStatus(), s.getOpeningBalance(), s.getExpectedBalance(),
                s.getActualBalance(), s.getDivergence(), s.getNotes(), movements,
                s.calculateTotalByMethod(PaymentMethod.DINHEIRO),
                s.calculateTotalByMethod(PaymentMethod.PIX),
                s.calculateTotalByMethod(PaymentMethod.CARTAO_DEBITO),
                s.calculateTotalByMethod(PaymentMethod.CARTAO_CREDITO),
                s.getOpenedAt(), s.getClosedAt()
        );
    }

    private PosDto.PosSaleResponse toSaleResponse(PosSale s) {
        List<PosDto.PosSaleItemResponse> items = s.getItems().stream().map(i ->
                new PosDto.PosSaleItemResponse(
                        i.getId(), i.getSkuId(), i.getSkuCode(), i.getTitle(),
                        i.getQuantity(), i.getUnitPrice(), i.getDiscount(), i.getSubtotal()
                )
        ).toList();

        List<PosDto.PosSalePaymentResponse> payments = s.getPayments().stream().map(p ->
                new PosDto.PosSalePaymentResponse(p.getId(), p.getPaymentMethod(), p.getAmount())
        ).toList();

        return new PosDto.PosSaleResponse(
                s.getId(), s.getTenantId(), s.getCompanyId(), s.getShiftId(),
                s.getSaleNumber(), s.getClientUuid(), s.getCustomerName(), s.getCustomerCpfCnpj(),
                s.getSubtotal(), s.getDiscount(), s.getTotal(), s.getTotalPaid(),
                s.getChangeAmount(), s.getStatus(), s.getNfceKey(), s.getNfceStatus(),
                items, payments, s.getCreatedBy(), s.getCreatedAt()
        );
    }
}

