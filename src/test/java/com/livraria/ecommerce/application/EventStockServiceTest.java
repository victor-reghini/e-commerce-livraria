package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.dto.stock.EventStockDto;
import com.livraria.ecommerce.application.service.EventStockService;
import com.livraria.ecommerce.domain.exception.BusinessException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - EventStockService (Aplicação de Estoque, Feiras, Romaneio e Auditoria)")
class EventStockServiceTest {

    @Mock
    private StockTransferRepositoryPort transferRepository;
    @Mock
    private BlindAuditRepositoryPort auditRepository;
    @Mock
    private StockMovementRepositoryPort movementRepository;
    @Mock
    private StockRepositoryPort stockRepository;
    @Mock
    private SpringDataStockItemRepository stockItemRepository;
    @Mock
    private SpringDataCompanyRepository companyRepository;
    @Mock
    private SpringDataSkuRepository skuRepository;
    @Mock
    private SpringDataProductRepository productRepository;

    @InjectMocks
    private EventStockService eventStockService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID originCompanyId = UUID.randomUUID();
    private final UUID destinationCompanyId = UUID.randomUUID();
    private final UUID skuId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateTransferAndReserveStockSuccessfully() {
        CompanyJpaEntity origin = CompanyJpaEntity.builder().id(originCompanyId).tradeName("Matriz").build();
        CompanyJpaEntity dest = CompanyJpaEntity.builder().id(destinationCompanyId).tradeName("Feira Bienal").build();

        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).productId(productId).skuCode("LIV-100")
                .price(new BigDecimal("45.00")).costPrice(new BigDecimal("20.00")).build();
        ProductJpaEntity product = ProductJpaEntity.builder().id(productId).title("Dom Casmurro").build();

        when(companyRepository.findById(originCompanyId)).thenReturn(Optional.of(origin));
        when(companyRepository.findById(destinationCompanyId)).thenReturn(Optional.of(dest));
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(stockRepository.getAvailableStock(tenantId, originCompanyId, skuId)).thenReturn(50);
        when(stockRepository.getPhysicalStock(tenantId, originCompanyId, skuId)).thenReturn(50);
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(i -> i.getArgument(0));

        EventStockDto.CreateTransferRequest request = new EventStockDto.CreateTransferRequest(
                originCompanyId, destinationCompanyId, TransferType.REMESSA_EVENTO, "Bienal do Livro",
                List.of(new EventStockDto.TransferItemRequest(skuId, 15, new BigDecimal("45.00")))
        );

        StockTransfer transfer = eventStockService.createTransfer(request, userId);

        assertNotNull(transfer);
        assertEquals(TransferStatus.RESERVADO, transfer.getStatus());
        assertEquals("Matriz", transfer.getOriginCompanyName());
        assertEquals("Feira Bienal", transfer.getDestinationCompanyName());

        verify(stockRepository).reserveStock(tenantId, originCompanyId, skuId, 15);
        verify(movementRepository).save(any(StockMovement.class));
        verify(transferRepository).save(any(StockTransfer.class));
    }

    @Test
    void shouldRejectTransferWhenAvailableStockIsInsufficient() {
        CompanyJpaEntity origin = CompanyJpaEntity.builder().id(originCompanyId).tradeName("Matriz").build();
        CompanyJpaEntity dest = CompanyJpaEntity.builder().id(destinationCompanyId).tradeName("Feira").build();
        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).productId(productId).skuCode("LIV-100").build();

        when(companyRepository.findById(originCompanyId)).thenReturn(Optional.of(origin));
        when(companyRepository.findById(destinationCompanyId)).thenReturn(Optional.of(dest));
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(stockRepository.getAvailableStock(tenantId, originCompanyId, skuId)).thenReturn(5); // apenas 5 disponíveis

        EventStockDto.CreateTransferRequest request = new EventStockDto.CreateTransferRequest(
                originCompanyId, destinationCompanyId, TransferType.REMESSA_EVENTO, null,
                List.of(new EventStockDto.TransferItemRequest(skuId, 10, BigDecimal.TEN)) // pede 10
        );

        assertThrows(BusinessException.class, () -> eventStockService.createTransfer(request, userId));
        verify(transferRepository, never()).save(any());
    }

    @Test
    void shouldDispatchTransferSuccessfully() {
        UUID transferId = UUID.randomUUID();
        StockTransferItem item = new StockTransferItem(
                UUID.randomUUID(), tenantId, transferId, skuId, "LIV-100", "Dom Casmurro",
                20, 0, 0, BigDecimal.TEN, new BigDecimal("30.00")
        );
        StockTransfer transfer = new StockTransfer(
                transferId, tenantId, "TRF-001", originCompanyId, "Matriz", destinationCompanyId, "Feira",
                TransferStatus.RESERVADO, TransferType.REMESSA_EVENTO, null,
                List.of(item), null, null, userId, null, Instant.now(), Instant.now()
        );

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(i -> i.getArgument(0));

        StockTransfer dispatched = eventStockService.dispatchTransfer(transferId, userId);

        assertEquals(TransferStatus.EM_TRANSITO, dispatched.getStatus());
        assertEquals(20, dispatched.getItems().get(0).getQuantityDispatched());
        verify(stockRepository).transferStock(tenantId, originCompanyId, destinationCompanyId, skuId, 20);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void shouldReceiveTransferWithDivergenceHandling() {
        UUID transferId = UUID.randomUUID();
        StockTransferItem item = new StockTransferItem(
                UUID.randomUUID(), tenantId, transferId, skuId, "LIV-100", "Dom Casmurro",
                20, 20, 0, BigDecimal.TEN, new BigDecimal("30.00")
        );
        StockTransfer transfer = new StockTransfer(
                transferId, tenantId, "TRF-001", originCompanyId, "Matriz", destinationCompanyId, "Feira",
                TransferStatus.EM_TRANSITO, TransferType.REMESSA_EVENTO, null,
                List.of(item), Instant.now(), null, userId, null, Instant.now(), Instant.now()
        );

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(transferRepository.save(any(StockTransfer.class))).thenAnswer(i -> i.getArgument(0));
        when(stockRepository.getPhysicalStock(tenantId, destinationCompanyId, skuId)).thenReturn(0);

        Map<UUID, Integer> received = new HashMap<>();
        received.put(skuId, 18); // Faltaram 2

        StockTransfer receivedTransfer = eventStockService.receiveTransfer(
                transferId, new EventStockDto.ReceiveTransferRequest(received), userId
        );

        assertEquals(TransferStatus.DIVERGENCIA, receivedTransfer.getStatus());
        assertEquals(18, item.getQuantityReceived());
        assertEquals(-2, item.getDivergence());

        verify(stockRepository).setStockQuantity(tenantId, destinationCompanyId, skuId, 18);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void shouldGenerateRomaneioAccurately() {
        UUID transferId = UUID.randomUUID();
        StockTransferItem item = new StockTransferItem(
                UUID.randomUUID(), tenantId, transferId, skuId, "LIV-100", "Dom Casmurro",
                10, 10, 0, BigDecimal.TEN, new BigDecimal("40.00")
        );
        StockTransfer transfer = new StockTransfer(
                transferId, tenantId, "TRF-999", originCompanyId, "Editora Matriz", destinationCompanyId, "Feira USP",
                TransferStatus.EM_TRANSITO, TransferType.REMESSA_EVENTO, "Carga frágil",
                List.of(item), Instant.now(), null, userId, null, Instant.now(), Instant.now()
        );

        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).isbn("9788535902778").weightGrams(400).build();

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));

        EventStockDto.RomaneioDocumentResponse romaneio = eventStockService.generateRomaneio(transferId);

        assertNotNull(romaneio);
        assertEquals("TRF-999", romaneio.transferNumber());
        assertEquals("Editora Matriz", romaneio.emitente());
        assertEquals("Feira USP", romaneio.destinatario());
        assertEquals(10, romaneio.quantidadeTotalLivros());
        assertEquals(4000, romaneio.pesoTotalGramas()); // 10 * 400g = 4000g
        assertEquals(new BigDecimal("400.00"), romaneio.valorTotalCarga()); // 10 * R$ 40,00 = R$ 400,00
        assertEquals(1, romaneio.itens().size());
        assertEquals("9788535902778", romaneio.itens().get(0).isbn());
    }

    @Test
    void shouldCreateBlindAuditAndScanBarcodes() {
        CompanyJpaEntity company = CompanyJpaEntity.builder().id(destinationCompanyId).tradeName("Feira Bienal").build();
        when(companyRepository.findById(destinationCompanyId)).thenReturn(Optional.of(company));
        when(auditRepository.save(any(BlindAudit.class))).thenAnswer(i -> i.getArgument(0));

        BlindAudit audit = eventStockService.createBlindAudit(
                new EventStockDto.CreateAuditRequest(destinationCompanyId, null, "Auditoria pós-evento"), userId
        );

        assertNotNull(audit);
        assertEquals(AuditStatus.ABERTA, audit.getStatus());

        // Bipagem
        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).productId(productId).skuCode("SKU-100").barcode("7891234567890").build();
        ProductJpaEntity product = ProductJpaEntity.builder().id(productId).title("Livro Bipado").build();

        when(auditRepository.findById(audit.getId())).thenReturn(Optional.of(audit));
        when(skuRepository.findByTenantIdAndBarcode(tenantId, "7891234567890")).thenReturn(Optional.of(sku));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        BlindAudit updatedAudit = eventStockService.scanBarcode(audit.getId(), "7891234567890", 3);

        assertEquals(AuditStatus.EM_BIPAGEM, updatedAudit.getStatus());
        assertEquals(3, updatedAudit.getTotalScanned());
        assertEquals(1, updatedAudit.getItems().size());
    }

    @Test
    void shouldFinalizeAndReconcileBlindAudit() {
        BlindAuditItem item = new BlindAuditItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId, "SKU-1", "Livro", 8, 10, -2, Instant.now()
        );
        BlindAudit audit = new BlindAudit(
                UUID.randomUUID(), tenantId, destinationCompanyId, "Feira", null, "AUD-01",
                AuditStatus.FINALIZADA, userId, null, List.of(item), Instant.now(), Instant.now()
        );

        when(auditRepository.findById(audit.getId())).thenReturn(Optional.of(audit));
        when(auditRepository.save(any(BlindAudit.class))).thenAnswer(i -> i.getArgument(0));
        when(stockRepository.getPhysicalStock(tenantId, destinationCompanyId, skuId)).thenReturn(10);

        BlindAudit reconciled = eventStockService.reconcileAudit(audit.getId(), userId);

        assertEquals(AuditStatus.CONCILIADA, reconciled.getStatus());
        // Estoque físico original era 10, com divergência -2, novo saldo deve ser 8
        verify(stockRepository).setStockQuantity(tenantId, destinationCompanyId, skuId, 8);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void shouldGenerateEventDashboardMetrics() {
        CompanyJpaEntity company = CompanyJpaEntity.builder().id(destinationCompanyId).tradeName("Feira USP").build();
        when(companyRepository.findById(destinationCompanyId)).thenReturn(Optional.of(company));

        StockTransferItem item = new StockTransferItem(
                UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId, "SKU-1", "Livro Top",
                100, 100, 100, BigDecimal.TEN, new BigDecimal("50.00")
        );
        StockTransfer transfer = new StockTransfer(
                UUID.randomUUID(), tenantId, "TRF-1", originCompanyId, "Matriz", destinationCompanyId, "Feira USP",
                TransferStatus.RECEBIDO, TransferType.REMESSA_EVENTO, null,
                List.of(item), Instant.now(), Instant.now(), userId, userId, Instant.now(), Instant.now()
        );
        when(transferRepository.findAllByCompanyId(tenantId, destinationCompanyId)).thenReturn(List.of(transfer));

        StockMovement sale = new StockMovement(
                UUID.randomUUID(), tenantId, destinationCompanyId, skuId,
                MovementType.VENDA_PDV, -40, 60, UUID.randomUUID(), "Venda balcão", userId, Instant.now()
        );
        when(movementRepository.findAllByCompanyId(tenantId, destinationCompanyId, 0, 1000)).thenReturn(List.of(sale));

        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).productId(productId).skuCode("SKU-1").price(new BigDecimal("50.00")).build();
        ProductJpaEntity prod = ProductJpaEntity.builder().id(productId).title("Livro Top").build();
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(productRepository.findById(productId)).thenReturn(Optional.of(prod));

        when(stockRepository.getTotalPhysicalStock(tenantId, destinationCompanyId)).thenReturn(60);

        StockItemJpaEntity alertItem = StockItemJpaEntity.builder()
                .skuId(skuId).quantity(5).minThreshold(10).build();
        when(stockItemRepository.findAllByTenantIdAndCompanyId(tenantId, destinationCompanyId)).thenReturn(List.of(alertItem));

        EventDashboard dashboard = eventStockService.getEventDashboard(destinationCompanyId);

        assertNotNull(dashboard);
        assertEquals("Feira USP", dashboard.companyName());
        assertEquals(100, dashboard.totalItemsDispatched());
        assertEquals(new BigDecimal("5000.00"), dashboard.totalValueDispatched());
        assertEquals(40, dashboard.totalItemsSold());
        assertEquals(60, dashboard.currentPhysicalStock());
        assertEquals(1, dashboard.activeAlertsCount());
        assertEquals(1, dashboard.topSellingItems().size());
        assertEquals(40, dashboard.topSellingItems().get(0).quantitySold());
    }
}
