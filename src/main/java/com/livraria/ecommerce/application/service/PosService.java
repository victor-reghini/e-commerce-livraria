package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.application.dto.pos.PosDto;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.pos.CashShift;
import com.livraria.ecommerce.domain.model.pos.NfceGenerator;
import com.livraria.ecommerce.domain.model.pos.NfceStatus;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.PosSalePayment;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import com.livraria.ecommerce.domain.model.pos.ShiftStatus;
import com.livraria.ecommerce.domain.model.stock.MovementType;
import com.livraria.ecommerce.domain.model.stock.StockMovement;
import com.livraria.ecommerce.domain.port.in.PosUseCase;
import com.livraria.ecommerce.domain.port.out.CashShiftRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PosSaleRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockMovementRepositoryPort;
import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataUserRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Transactional
public class PosService implements PosUseCase {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final AtomicInteger SHIFT_SEQ = new AtomicInteger(100);
    private static final AtomicInteger SALE_SEQ = new AtomicInteger(500);

    private final CashShiftRepositoryPort shiftRepository;
    private final PosSaleRepositoryPort saleRepository;
    private final StockRepositoryPort stockRepository;
    private final StockMovementRepositoryPort movementRepository;
    private final SpringDataCompanyRepository companyRepository;
    private final SpringDataSkuRepository skuRepository;
    private final SpringDataProductRepository productRepository;
    private final SpringDataUserRepository userRepository;

    public PosService(CashShiftRepositoryPort shiftRepository,
                      PosSaleRepositoryPort saleRepository,
                      StockRepositoryPort stockRepository,
                      StockMovementRepositoryPort movementRepository,
                      SpringDataCompanyRepository companyRepository,
                      SpringDataSkuRepository skuRepository,
                      SpringDataProductRepository productRepository,
                      SpringDataUserRepository userRepository) {
        this.shiftRepository = shiftRepository;
        this.saleRepository = saleRepository;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.companyRepository = companyRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private UUID getTenantId() {
        return TenantContext.getTenantId().orElse(DEFAULT_TENANT_ID);
    }

    @Override
    public CashShift openShift(UUID companyId, BigDecimal openingBalance, UUID cashierId) {
        UUID tenantId = getTenantId();

        Optional<CashShift> existing = shiftRepository.findCurrentOpenShift(tenantId, companyId, cashierId);
        if (existing.isPresent()) {
            return existing.get();
        }

        CompanyJpaEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Filial não encontrada: ", companyId));
        String cashierName = userRepository.findById(cashierId)
                .map(UserJpaEntity::getName).orElse("Operador " + cashierId);

        String shiftNumber = "CX-" + System.currentTimeMillis() % 1000000 + "-" + SHIFT_SEQ.incrementAndGet();
        BigDecimal initialBal = openingBalance != null ? openingBalance : BigDecimal.ZERO;

        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, company.getId(), cashierId, cashierName,
                shiftNumber, ShiftStatus.ABERTO, initialBal, null, null, null,
                "Abertura de turno de caixa", new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        if (initialBal.compareTo(BigDecimal.ZERO) > 0) {
            shift.addSupply(initialBal, "Fundo de troco inicial", cashierId);
        }

        return shiftRepository.save(shift);
    }

    @Override
    public CashShift supplyCash(UUID shiftId, BigDecimal amount, String reason, UUID operatorId) {
        CashShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Turno de caixa não encontrado: ", shiftId));

        shift.addSupply(amount, reason, operatorId);
        return shiftRepository.save(shift);
    }

    @Override
    public CashShift bleedCash(UUID shiftId, BigDecimal amount, String reason, UUID operatorId) {
        CashShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Turno de caixa não encontrado: ", shiftId));

        shift.addBleed(amount, reason, operatorId);
        return shiftRepository.save(shift);
    }

    @Override
    public CashShift closeShift(UUID shiftId, BigDecimal countedCash, String notes, UUID operatorId) {
        CashShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Turno de caixa não encontrado: ", shiftId));

        shift.closeBlind(countedCash, notes);
        return shiftRepository.save(shift);
    }

    @Override
    @Transactional(readOnly = true)
    public CashShift getShiftById(UUID shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Turno de caixa não encontrado: ", shiftId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashShift> getCurrentOpenShift(UUID companyId, UUID cashierId) {
        return shiftRepository.findCurrentOpenShift(getTenantId(), companyId, cashierId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashShift> listShifts(UUID companyId, int page, int size) {
        return shiftRepository.findAllByCompanyId(getTenantId(), companyId, page, size);
    }

    @Override
    public PosSale createSale(PosDto.CreateSaleRequest request, UUID cashierId) {
        UUID tenantId = getTenantId();

        // 1. Idempotência para vendas offline
        if (request.clientUuid() != null) {
            Optional<PosSale> existing = saleRepository.findByClientUuid(tenantId, request.clientUuid());
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        // 2. Resolução do turno de caixa
        CashShift shift;
        if (request.shiftId() != null) {
            shift = shiftRepository.findById(request.shiftId())
                    .orElseThrow(() -> new EntityNotFoundException("Turno de caixa não encontrado: ", request.shiftId()));
            if (shift.getStatus() != ShiftStatus.ABERTO) {
                throw new BusinessException("Não é possível realizar venda em um turno de caixa FECHADO.");
            }
        } else {
            shift = shiftRepository.findCurrentOpenShift(tenantId, request.companyId(), cashierId)
                    .orElseGet(() -> openShift(request.companyId(), BigDecimal.ZERO, cashierId));
        }

        CompanyJpaEntity company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new EntityNotFoundException("Filial não encontrada: ", request.companyId()));

        // 3. Validação de estoque e montagem dos itens
        List<PosSaleItem> items = new ArrayList<>();
        UUID saleId = UUID.randomUUID();

        for (PosDto.SaleItemRequest itemReq : request.items()) {
            SkuJpaEntity sku = skuRepository.findById(itemReq.skuId())
                    .orElseThrow(() -> new EntityNotFoundException("SKU não encontrado: ", itemReq.skuId()));

            String title = productRepository.findById(sku.getProductId())
                    .map(ProductJpaEntity::getTitle).orElse(sku.getSkuCode());

            int available = stockRepository.getAvailableStock(tenantId, request.companyId(), sku.getId());
            if (available < itemReq.quantity()) {
                throw new BusinessException("Estoque disponível insuficiente no balcão para o SKU '" + sku.getSkuCode()
                        + "'. Disponível: " + available + ", Solicitado: " + itemReq.quantity());
            }

            BigDecimal price = itemReq.unitPrice() != null ? itemReq.unitPrice() : sku.getPrice();
            BigDecimal discount = itemReq.discount() != null ? itemReq.discount() : BigDecimal.ZERO;
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemReq.quantity())).subtract(discount);

            items.add(new PosSaleItem(
                    UUID.randomUUID(), tenantId, saleId, sku.getId(), sku.getSkuCode(), title,
                    itemReq.quantity(), price, discount, subtotal
            ));
        }

        // 4. Montagem dos pagamentos
        List<PosSalePayment> payments = new ArrayList<>();
        for (PosDto.SalePaymentRequest pReq : request.payments()) {
            payments.add(new PosSalePayment(
                    UUID.randomUUID(), tenantId, saleId, pReq.paymentMethod(), pReq.amount()
            ));
        }

        // 5. Geração da Venda e NFC-e
        String saleNumber = "VD-" + System.currentTimeMillis() % 1000000 + "-" + SALE_SEQ.incrementAndGet();
        long numericSaleNum = System.currentTimeMillis() % 1000000L;
        String cleanCnpj = company.getCnpj() != null ? company.getCnpj().replaceAll("\\D", "") : "12345678000195";
        String accessKey = NfceGenerator.generateAccessKey(cleanCnpj, 1, numericSaleNum, false);

        PosSale sale = new PosSale(
                saleId, tenantId, company.getId(), shift.getId(), saleNumber,
                request.clientUuid(), request.customerName(), request.customerCpfCnpj(),
                BigDecimal.ZERO, request.discount(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                SaleStatus.CONCLUIDA, accessKey, null, NfceStatus.EMITIDA,
                items, payments, cashierId, Instant.now(), Instant.now()
        );

        String sampleXml = NfceGenerator.buildSampleNfceXml(accessKey, sale, company.getTradeName(), cleanCnpj);
        sale.setNfceData(accessKey, sampleXml, NfceStatus.EMITIDA);

        PosSale savedSale = saleRepository.save(sale);

        // 6. Baixa de estoque física e registro no ledger contábil
        for (PosSaleItem item : items) {
            int currentStock = stockRepository.getPhysicalStock(tenantId, request.companyId(), item.getSkuId());
            int newStock = Math.max(0, currentStock - item.getQuantity());
            stockRepository.setStockQuantity(tenantId, request.companyId(), item.getSkuId(), newStock);

            movementRepository.save(new StockMovement(
                    UUID.randomUUID(), tenantId, request.companyId(), item.getSkuId(),
                    MovementType.VENDA_PDV, -item.getQuantity(), newStock,
                    savedSale.getId(), "Venda PDV " + savedSale.getSaleNumber(), cashierId, Instant.now()
            ));
        }

        // 7. Registro dos pagamentos no turno de caixa
        for (PosSalePayment payment : payments) {
            shift.recordSalePayment(payment.getAmount(), payment.getPaymentMethod(), cashierId);
        }
        shiftRepository.save(shift);

        return savedSale;
    }

    @Override
    public PosSale cancelSale(UUID saleId, String reason, UUID operatorId) {
        UUID tenantId = getTenantId();
        PosSale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada: ", saleId));

        sale.cancel();

        // Estorno no estoque físico
        for (PosSaleItem item : sale.getItems()) {
            int currentStock = stockRepository.getPhysicalStock(tenantId, sale.getCompanyId(), item.getSkuId());
            int newStock = currentStock + item.getQuantity();
            stockRepository.setStockQuantity(tenantId, sale.getCompanyId(), item.getSkuId(), newStock);

            movementRepository.save(new StockMovement(
                    UUID.randomUUID(), tenantId, sale.getCompanyId(), item.getSkuId(),
                    MovementType.ENTRADA_AVULSA, item.getQuantity(), newStock,
                    sale.getId(), "Cancelamento da venda " + sale.getSaleNumber() + ": " + (reason != null ? reason : "Estorno balcão"),
                    operatorId, Instant.now()
            ));
        }

        return saleRepository.save(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public PosSale getSaleById(UUID saleId) {
        return saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venda não encontrada: ", saleId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosSale> listSalesByShift(UUID shiftId) {
        return saleRepository.findAllByShiftId(shiftId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosSale> listSalesByCompany(UUID companyId, int page, int size) {
        return saleRepository.findAllByCompanyId(getTenantId(), companyId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PosDto.OfflineCatalogSyncResponse syncCatalog(UUID companyId, Instant updatedSince) {
        UUID tenantId = getTenantId();
        List<SkuJpaEntity> skus = skuRepository.findAll();
        List<PosDto.OfflineCatalogItem> catalogItems = new ArrayList<>();

        for (SkuJpaEntity sku : skus) {
            if (sku.getTenantId().equals(tenantId) && sku.isActive()) {
                String title = productRepository.findById(sku.getProductId())
                        .map(ProductJpaEntity::getTitle).orElse(sku.getSkuCode());

                int physicalStock = stockRepository.getPhysicalStock(tenantId, companyId, sku.getId());
                int availableStock = stockRepository.getAvailableStock(tenantId, companyId, sku.getId());

                catalogItems.add(new PosDto.OfflineCatalogItem(
                        sku.getId(),
                        sku.getProductId(),
                        sku.getSkuCode(),
                        sku.getBarcode(),
                        sku.getIsbn(),
                        title,
                        sku.getPrice(),
                        sku.getPromotionalPrice(),
                        physicalStock,
                        availableStock
                ));
            }
        }

        return new PosDto.OfflineCatalogSyncResponse(
                companyId, Instant.now(), catalogItems.size(), catalogItems
        );
    }

    @Override
    public PosDto.OfflineBatchSyncResult syncOfflineSales(List<PosDto.CreateSaleRequest> sales, UUID cashierId) {
        UUID tenantId = getTenantId();
        int total = sales != null ? sales.size() : 0;
        int processed = 0;
        int duplicates = 0;
        int failures = 0;
        List<PosDto.PosSaleResponse> processedResponses = new ArrayList<>();

        if (sales != null) {
            for (PosDto.CreateSaleRequest req : sales) {
                try {
                    if (req.clientUuid() != null && saleRepository.findByClientUuid(tenantId, req.clientUuid()).isPresent()) {
                        duplicates++;
                    } else {
                        PosSale created = createSale(req, cashierId);
                        processed++;
                        processedResponses.add(toSaleResponse(created));
                    }
                } catch (Exception e) {
                    failures++;
                }
            }
        }

        return new PosDto.OfflineBatchSyncResult(total, processed, duplicates, failures, processedResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public PosDto.ThermalReceiptResponse getThermalReceipt(UUID saleId) {
        PosSale sale = getSaleById(saleId);
        CompanyJpaEntity company = companyRepository.findById(sale.getCompanyId())
                .orElse(null);

        String compName = company != null ? company.getTradeName() : "Livraria & Editora";
        String compCnpj = company != null ? company.getCnpj() : "00.000.000/0001-00";

        List<PosDto.ThermalReceiptResponse.ReceiptItem> receiptItems = sale.getItems().stream().map(i ->
                new PosDto.ThermalReceiptResponse.ReceiptItem(
                        i.getSkuCode(), i.getTitle(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal()
                )
        ).toList();

        List<PosDto.ThermalReceiptResponse.ReceiptPayment> receiptPayments = sale.getPayments().stream().map(p ->
                new PosDto.ThermalReceiptResponse.ReceiptPayment(
                        p.getPaymentMethod(), p.getAmount()
                )
        ).toList();

        String qrUrl = "https://www.fazenda.sp.gov.br/nfce/qrcode?p=" + sale.getNfceKey();

        return new PosDto.ThermalReceiptResponse(
                sale.getSaleNumber(),
                compName,
                compCnpj,
                "Rua das Letras, 100 - Centro",
                sale.getCreatedAt(),
                receiptItems,
                sale.getSubtotal(),
                sale.getDiscount(),
                sale.getTotal(),
                receiptPayments,
                sale.getChangeAmount(),
                sale.getNfceKey(),
                qrUrl,
                "Operador " + sale.getCreatedBy()
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
