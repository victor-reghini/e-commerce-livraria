package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.application.dto.stock.EventStockDto;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.stock.AuditStatus;
import com.livraria.ecommerce.domain.model.stock.BlindAudit;
import com.livraria.ecommerce.domain.model.stock.BlindAuditItem;
import com.livraria.ecommerce.domain.model.stock.EventDashboard;
import com.livraria.ecommerce.domain.model.stock.MovementType;
import com.livraria.ecommerce.domain.model.stock.StockMovement;
import com.livraria.ecommerce.domain.model.stock.StockTransfer;
import com.livraria.ecommerce.domain.model.stock.StockTransferItem;
import com.livraria.ecommerce.domain.model.stock.TransferStatus;
import com.livraria.ecommerce.domain.model.stock.TransferType;
import com.livraria.ecommerce.domain.port.in.EventStockUseCase;
import com.livraria.ecommerce.domain.port.out.BlindAuditRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockMovementRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockTransferRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class EventStockService implements EventStockUseCase {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final AtomicInteger TRANSFER_SEQ = new AtomicInteger(1000);
    private static final AtomicInteger AUDIT_SEQ = new AtomicInteger(2000);

    private final StockTransferRepositoryPort transferRepository;
    private final BlindAuditRepositoryPort auditRepository;
    private final StockMovementRepositoryPort movementRepository;
    private final StockRepositoryPort stockRepository;
    private final SpringDataStockItemRepository stockItemRepository;
    private final SpringDataCompanyRepository companyRepository;
    private final SpringDataSkuRepository skuRepository;
    private final SpringDataProductRepository productRepository;

    public EventStockService(StockTransferRepositoryPort transferRepository,
                             BlindAuditRepositoryPort auditRepository,
                             StockMovementRepositoryPort movementRepository,
                             StockRepositoryPort stockRepository,
                             SpringDataStockItemRepository stockItemRepository,
                             SpringDataCompanyRepository companyRepository,
                             SpringDataSkuRepository skuRepository,
                             SpringDataProductRepository productRepository) {
        this.transferRepository = transferRepository;
        this.auditRepository = auditRepository;
        this.movementRepository = movementRepository;
        this.stockRepository = stockRepository;
        this.stockItemRepository = stockItemRepository;
        this.companyRepository = companyRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
    }

    private UUID getTenantId() {
        return TenantContext.getTenantId().orElse(DEFAULT_TENANT_ID);
    }

    @Override
    public StockTransfer createTransfer(EventStockDto.CreateTransferRequest request, UUID createdBy) {
        UUID tenantId = getTenantId();

        if (request.originCompanyId().equals(request.destinationCompanyId())) {
            throw new BusinessException("A filial de origem e de destino não podem ser iguais.");
        }

        CompanyJpaEntity origin = companyRepository.findById(request.originCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Filial de origem não encontrada: ", request.originCompanyId()));
        CompanyJpaEntity destination = companyRepository.findById(request.destinationCompanyId())
                .orElseThrow(() -> new EntityNotFoundException("Filial de destino não encontrada: ", request.destinationCompanyId()));

        String transferNumber = "TRF-" + System.currentTimeMillis() % 1000000 + "-" + TRANSFER_SEQ.incrementAndGet();
        UUID transferId = UUID.randomUUID();

        List<StockTransferItem> items = new ArrayList<>();
        for (EventStockDto.TransferItemRequest itemReq : request.items()) {
            SkuJpaEntity sku = skuRepository.findById(itemReq.skuId())
                    .orElseThrow(() -> new EntityNotFoundException("SKU não encontrado: ", itemReq.skuId()));

            String title = productRepository.findById(sku.getProductId())
                    .map(ProductJpaEntity::getTitle).orElse(sku.getSkuCode());

            int availableStock = stockRepository.getAvailableStock(tenantId, request.originCompanyId(), sku.getId());
            if (availableStock < itemReq.quantity()) {
                throw new BusinessException("Estoque disponível insuficiente para o SKU '" + sku.getSkuCode()
                        + "'. Disponível: " + availableStock + ", Solicitado: " + itemReq.quantity());
            }

            // Reserve stock at origin
            stockRepository.reserveStock(tenantId, request.originCompanyId(), sku.getId(), itemReq.quantity());

            int balanceAfter = stockRepository.getPhysicalStock(tenantId, request.originCompanyId(), sku.getId());
            movementRepository.save(new StockMovement(
                    UUID.randomUUID(), tenantId, request.originCompanyId(), sku.getId(),
                    MovementType.RESERVA, -itemReq.quantity(), balanceAfter,
                    transferId, "Reserva para transferência " + transferNumber, createdBy, Instant.now()
            ));

            BigDecimal price = itemReq.unitPrice() != null ? itemReq.unitPrice() : sku.getPrice();
            items.add(new StockTransferItem(
                    UUID.randomUUID(), tenantId, transferId, sku.getId(), sku.getSkuCode(), title,
                    itemReq.quantity(), 0, 0, sku.getCostPrice(), price
            ));
        }

        StockTransfer transfer = new StockTransfer(
                transferId, tenantId, transferNumber,
                origin.getId(), origin.getTradeName(),
                destination.getId(), destination.getTradeName(),
                TransferStatus.RESERVADO,
                request.transferType() != null ? request.transferType() : TransferType.REMESSA_EVENTO,
                request.notes(), items, null, null, createdBy, null, Instant.now(), Instant.now()
        );

        return transferRepository.save(transfer);
    }

    @Override
    public StockTransfer dispatchTransfer(UUID transferId, UUID dispatchedBy) {
        UUID tenantId = getTenantId();
        StockTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Transferência não encontrada: ", transferId));

        transfer.dispatch(dispatchedBy);

        // Deduct physical and reserved stock from origin
        for (StockTransferItem item : transfer.getItems()) {
            stockRepository.transferStock(tenantId, transfer.getOriginCompanyId(), transfer.getDestinationCompanyId(),
                    item.getSkuId(), item.getQuantityDispatched());

            // Notice transferStock moved stock from origin to destination. But wait:
            // In physical transit, destination has not received it yet.
            // Let's adjust destination physical stock back down until receiveTransfer!
            // Or let's handle origin deduction and destination addition explicitly:
            int destStock = stockRepository.getPhysicalStock(tenantId, transfer.getDestinationCompanyId(), item.getSkuId());
            stockRepository.setStockQuantity(tenantId, transfer.getDestinationCompanyId(), item.getSkuId(),
                    Math.max(0, destStock - item.getQuantityDispatched()));

            int balanceAfter = stockRepository.getPhysicalStock(tenantId, transfer.getOriginCompanyId(), item.getSkuId());
            movementRepository.save(new StockMovement(
                    UUID.randomUUID(), tenantId, transfer.getOriginCompanyId(), item.getSkuId(),
                    MovementType.TRANSFERENCIA_SAIDA, -item.getQuantityDispatched(), balanceAfter,
                    transfer.getId(), "Despacho de transferência " + transfer.getTransferNumber(), dispatchedBy, Instant.now()
            ));
        }

        return transferRepository.save(transfer);
    }

    @Override
    public StockTransfer receiveTransfer(UUID transferId, EventStockDto.ReceiveTransferRequest request, UUID receivedBy) {
        UUID tenantId = getTenantId();
        StockTransfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Transferência não encontrada: ", transferId));

        Map<UUID, Integer> receivedMap = request != null ? request.receivedQuantities() : null;
        transfer.receive(receivedMap, receivedBy);

        for (StockTransferItem item : transfer.getItems()) {
            int receivedQty = item.getQuantityReceived();
            if (receivedQty > 0) {
                int currentDestStock = stockRepository.getPhysicalStock(tenantId, transfer.getDestinationCompanyId(), item.getSkuId());
                int newDestStock = currentDestStock + receivedQty;
                stockRepository.setStockQuantity(tenantId, transfer.getDestinationCompanyId(), item.getSkuId(), newDestStock);

                movementRepository.save(new StockMovement(
                        UUID.randomUUID(), tenantId, transfer.getDestinationCompanyId(), item.getSkuId(),
                        MovementType.TRANSFERENCIA_ENTRADA, receivedQty, newDestStock,
                        transfer.getId(), "Recebimento de transferência " + transfer.getTransferNumber(), receivedBy, Instant.now()
                ));
            }
        }

        return transferRepository.save(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public StockTransfer getTransferById(UUID transferId) {
        return transferRepository.findById(transferId)
                .orElseThrow(() -> new EntityNotFoundException("Transferência não encontrada: ", transferId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransfer> listTransfersByCompany(UUID companyId) {
        return transferRepository.findAllByCompanyId(getTenantId(), companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransfer> listAllTransfers() {
        return transferRepository.findAllByTenantId(getTenantId());
    }

    @Override
    @Transactional(readOnly = true)
    public EventStockDto.RomaneioDocumentResponse generateRomaneio(UUID transferId) {
        StockTransfer transfer = getTransferById(transferId);

        List<EventStockDto.RomaneioDocumentResponse.RomaneioItem> romaneioItens = new ArrayList<>();
        int pesoTotal = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;
        int totalLivros = 0;

        for (StockTransferItem item : transfer.getItems()) {
            SkuJpaEntity sku = skuRepository.findById(item.getSkuId()).orElse(null);
            int weight = sku != null ? sku.getWeightGrams() : 350;
            String isbn = sku != null && sku.getIsbn() != null ? sku.getIsbn() : "-";
            int qty = item.getQuantityDispatched() > 0 ? item.getQuantityDispatched() : item.getQuantityRequested();
            BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(qty));

            totalLivros += qty;
            pesoTotal += (weight * qty);
            valorTotal = valorTotal.add(subtotal);

            romaneioItens.add(new EventStockDto.RomaneioDocumentResponse.RomaneioItem(
                    item.getSkuCode(), isbn, item.getTitle(), qty, weight, item.getUnitPrice(), subtotal
            ));
        }

        return new EventStockDto.RomaneioDocumentResponse(
                transfer.getTransferNumber(),
                transfer.getOriginCompanyName(),
                transfer.getDestinationCompanyName(),
                transfer.getTransferType(),
                transfer.getDispatchedAt() != null ? transfer.getDispatchedAt() : transfer.getCreatedAt(),
                romaneioItens,
                totalLivros,
                pesoTotal,
                valorTotal,
                transfer.getNotes() != null ? transfer.getNotes() : "Romaneio de transporte e movimentação de estoque entre unidades."
        );
    }

    @Override
    public BlindAudit createBlindAudit(EventStockDto.CreateAuditRequest request, UUID auditorId) {
        UUID tenantId = getTenantId();

        CompanyJpaEntity company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("Filial não encontrada: ", request.companyId()));

        String code = "AUD-" + System.currentTimeMillis() % 1000000 + "-" + AUDIT_SEQ.incrementAndGet();
        UUID auditId = UUID.randomUUID();

        List<BlindAuditItem> items = new ArrayList<>();
        if (request.transferId() != null) {
            StockTransfer transfer = transferRepository.findById(request.transferId())
                    .orElseThrow(() -> new EntityNotFoundException("Transferência não encontrada: ", request.transferId()));

            for (StockTransferItem tItem : transfer.getItems()) {
                int expected = tItem.getQuantityDispatched() > 0 ? tItem.getQuantityDispatched() : tItem.getQuantityRequested();
                items.add(new BlindAuditItem(
                        UUID.randomUUID(), tenantId, auditId, tItem.getSkuId(),
                        tItem.getSkuCode(), tItem.getTitle(),
                        0, expected, -expected, Instant.now()
                ));
            }
        }

        BlindAudit audit = new BlindAudit(
                auditId, tenantId, company.getId(), company.getTradeName(),
                request.transferId(), code, AuditStatus.ABERTA,
                auditorId, request.notes(), items, Instant.now(), null
        );

        return auditRepository.save(audit);
    }

    @Override
    public BlindAudit scanBarcode(UUID auditId, String barcode, int quantity) {
        UUID tenantId = getTenantId();
        BlindAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new EntityNotFoundException("Conferência cega não encontrada: ", auditId));

        if (barcode == null || barcode.isBlank()) {
            throw new BusinessException("Código de barras não pode ser vazio.");
        }

        String cleanBarcode = barcode.trim();
        Optional<SkuJpaEntity> skuOpt = skuRepository.findByTenantIdAndBarcode(tenantId, cleanBarcode);
        if (skuOpt.isEmpty()) {
            skuOpt = skuRepository.findByTenantIdAndIsbn(tenantId, cleanBarcode);
        }
        if (skuOpt.isEmpty()) {
            skuOpt = skuRepository.findByTenantIdAndSkuCode(tenantId, cleanBarcode);
        }

        SkuJpaEntity sku = skuOpt.orElseThrow(() -> new BusinessException("SKU não encontrado com o código / ISBN: " + cleanBarcode));

        String title = productRepository.findById(sku.getProductId())
                .map(ProductJpaEntity::getTitle).orElse(sku.getSkuCode());

        audit.scanBarcode(sku.getId(), sku.getSkuCode(), title, Math.max(1, quantity));
        return auditRepository.save(audit);
    }

    @Override
    public BlindAudit finalizeAudit(UUID auditId) {
        BlindAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new EntityNotFoundException("Conferência cega não encontrada: ", auditId));

        audit.finalizeAudit();
        return auditRepository.save(audit);
    }

    @Override
    public BlindAudit reconcileAudit(UUID auditId, UUID reconciledBy) {
        UUID tenantId = getTenantId();
        BlindAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new EntityNotFoundException("Conferência cega não encontrada: ", auditId));

        audit.reconcile();

        for (BlindAuditItem item : audit.getItems()) {
            if (item.getDivergence() != 0) {
                int currentStock = stockRepository.getPhysicalStock(tenantId, audit.getCompanyId(), item.getSkuId());
                int adjustedStock = Math.max(0, currentStock + item.getDivergence());
                stockRepository.setStockQuantity(tenantId, audit.getCompanyId(), item.getSkuId(), adjustedStock);

                movementRepository.save(new StockMovement(
                        UUID.randomUUID(), tenantId, audit.getCompanyId(), item.getSkuId(),
                        MovementType.AJUSTE_AUDITORIA, item.getDivergence(), adjustedStock,
                        audit.getId(), "Conciliação da conferência cega " + audit.getCode(), reconciledBy, Instant.now()
                ));
            }
        }

        return auditRepository.save(audit);
    }

    @Override
    @Transactional(readOnly = true)
    public BlindAudit getBlindAuditById(UUID auditId) {
        return auditRepository.findById(auditId)
                .orElseThrow(() -> new EntityNotFoundException("Conferência cega não encontrada: ", auditId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlindAudit> listBlindAuditsByCompany(UUID companyId) {
        return auditRepository.findAllByCompanyId(getTenantId(), companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovement> listMovementsByCompany(UUID companyId, int page, int size) {
        return movementRepository.findAllByCompanyId(getTenantId(), companyId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDashboard getEventDashboard(UUID companyId) {
        UUID tenantId = getTenantId();
        CompanyJpaEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Filial/Feira não encontrada: ", companyId));

        List<StockTransfer> transfers = transferRepository.findAllByCompanyId(tenantId, companyId);
        int totalDispatchedItems = 0;
        BigDecimal totalDispatchedValue = BigDecimal.ZERO;

        for (StockTransfer t : transfers) {
            if (t.getDestinationCompanyId().equals(companyId) && t.getStatus() != TransferStatus.CANCELADO) {
                for (StockTransferItem item : t.getItems()) {
                    int qty = item.getQuantityDispatched() > 0 ? item.getQuantityDispatched() : item.getQuantityRequested();
                    totalDispatchedItems += qty;
                    totalDispatchedValue = totalDispatchedValue.add(item.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
                }
            }
        }

        List<StockMovement> movements = movementRepository.findAllByCompanyId(tenantId, companyId, 0, 1000);
        int totalSold = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<UUID, Integer> salesBySku = new HashMap<>();

        for (StockMovement m : movements) {
            if (m.getMovementType() == MovementType.VENDA_PDV || m.getMovementType() == MovementType.VENDA_ECOMMERCE) {
                int sold = Math.abs(m.getQuantityChange());
                totalSold += sold;
                salesBySku.put(m.getSkuId(), salesBySku.getOrDefault(m.getSkuId(), 0) + sold);
            }
        }

        List<EventDashboard.TopSellingItem> topSelling = new ArrayList<>();
        salesBySku.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(entry -> {
                    SkuJpaEntity sku = skuRepository.findById(entry.getKey()).orElse(null);
                    String skuCode = sku != null ? sku.getSkuCode() : entry.getKey().toString();
                    String title = sku != null ? productRepository.findById(sku.getProductId())
                            .map(ProductJpaEntity::getTitle).orElse(skuCode) : skuCode;
                    BigDecimal price = sku != null ? sku.getPrice() : BigDecimal.ZERO;
                    BigDecimal rev = price.multiply(BigDecimal.valueOf(entry.getValue()));

                    topSelling.add(new EventDashboard.TopSellingItem(entry.getKey(), skuCode, title, entry.getValue(), rev));
                });

        int currentPhysicalStock = stockRepository.getTotalPhysicalStock(tenantId, companyId);

        List<StockItemJpaEntity> allStock = stockItemRepository.findAllByTenantIdAndCompanyId(tenantId, companyId);
        List<EventDashboard.StockAlertItem> lowStockAlerts = new ArrayList<>();
        for (StockItemJpaEntity s : allStock) {
            if (s.getQuantity() <= s.getMinThreshold() && s.getMinThreshold() > 0) {
                SkuJpaEntity sku = skuRepository.findById(s.getSkuId()).orElse(null);
                String skuCode = sku != null ? sku.getSkuCode() : s.getSkuId().toString();
                String title = sku != null ? productRepository.findById(sku.getProductId())
                        .map(ProductJpaEntity::getTitle).orElse(skuCode) : skuCode;

                lowStockAlerts.add(new EventDashboard.StockAlertItem(s.getSkuId(), skuCode, title, s.getQuantity(), s.getMinThreshold()));
            }
        }

        return new EventDashboard(
                company.getId(),
                company.getTradeName(),
                totalDispatchedItems,
                totalDispatchedValue,
                totalSold,
                totalRevenue,
                currentPhysicalStock,
                lowStockAlerts.size(),
                topSelling,
                lowStockAlerts
        );
    }
}

