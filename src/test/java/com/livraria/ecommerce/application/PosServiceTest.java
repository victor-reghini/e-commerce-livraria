package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.dto.pos.PosDto;
import com.livraria.ecommerce.application.service.PosService;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.pos.CashShift;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.PosSalePayment;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import com.livraria.ecommerce.domain.model.pos.ShiftStatus;
import com.livraria.ecommerce.domain.model.stock.StockMovement;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - PosService (Serviço de Turnos de Caixa, Venda Balcão, Estoque e Offline)")
class PosServiceTest {

    @Mock
    private CashShiftRepositoryPort shiftRepository;
    @Mock
    private PosSaleRepositoryPort saleRepository;
    @Mock
    private StockRepositoryPort stockRepository;
    @Mock
    private StockMovementRepositoryPort movementRepository;
    @Mock
    private SpringDataCompanyRepository companyRepository;
    @Mock
    private SpringDataSkuRepository skuRepository;
    @Mock
    private SpringDataProductRepository productRepository;
    @Mock
    private SpringDataUserRepository userRepository;

    @InjectMocks
    private PosService posService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID cashierId = UUID.randomUUID();
    private final UUID skuId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldOpenShiftAndRegisterInitialSupply() {
        CompanyJpaEntity company = CompanyJpaEntity.builder().id(companyId).tradeName("Livraria Centro").build();
        UserJpaEntity cashier = UserJpaEntity.builder().id(cashierId).name("Carlos Balcão").build();

        when(shiftRepository.findCurrentOpenShift(tenantId, companyId, cashierId)).thenReturn(Optional.empty());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findById(cashierId)).thenReturn(Optional.of(cashier));
        when(shiftRepository.save(any(CashShift.class))).thenAnswer(i -> i.getArgument(0));

        CashShift shift = posService.openShift(companyId, new BigDecimal("150.00"), cashierId);

        assertNotNull(shift);
        assertEquals(ShiftStatus.ABERTO, shift.getStatus());
        assertEquals(new BigDecimal("150.00"), shift.getOpeningBalance());
        assertEquals("Carlos Balcão", shift.getCashierName());
        assertEquals(1, shift.getMovements().size());
        verify(shiftRepository).save(any(CashShift.class));
    }

    @Test
    void shouldReturnExistingShiftIfAlreadyOpen() {
        CashShift existing = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "Carlos", "CX-99",
                ShiftStatus.ABERTO, BigDecimal.TEN, null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );
        when(shiftRepository.findCurrentOpenShift(tenantId, companyId, cashierId)).thenReturn(Optional.of(existing));

        CashShift shift = posService.openShift(companyId, BigDecimal.ZERO, cashierId);

        assertSame(existing, shift);
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void shouldPerformSupplyAndBleedInCashShift() {
        UUID shiftId = UUID.randomUUID();
        CashShift shift = new CashShift(
                shiftId, tenantId, companyId, cashierId, "Carlos", "CX-01",
                ShiftStatus.ABERTO, new BigDecimal("200.00"), null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(shiftRepository.save(any(CashShift.class))).thenAnswer(i -> i.getArgument(0));

        // Suprimento de R$ 50,00
        CashShift afterSupply = posService.supplyCash(shiftId, new BigDecimal("50.00"), "Troco moedas", cashierId);
        assertEquals(new BigDecimal("250.00"), afterSupply.calculateExpectedCashBalance());

        // Sangria de R$ 100,00
        CashShift afterBleed = posService.bleedCash(shiftId, new BigDecimal("100.00"), "Cofre", cashierId);
        assertEquals(new BigDecimal("150.00"), afterBleed.calculateExpectedCashBalance());
    }

    @Test
    void shouldCloseShiftBlindly() {
        UUID shiftId = UUID.randomUUID();
        CashShift shift = new CashShift(
                shiftId, tenantId, companyId, cashierId, "Carlos", "CX-01",
                ShiftStatus.ABERTO, new BigDecimal("100.00"), null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(shiftRepository.save(any(CashShift.class))).thenAnswer(i -> i.getArgument(0));

        CashShift closed = posService.closeShift(shiftId, new BigDecimal("98.50"), "Fechamento regular", cashierId);

        assertEquals(ShiftStatus.FECHADO, closed.getStatus());
        assertEquals(new BigDecimal("100.00"), closed.getExpectedBalance());
        assertEquals(new BigDecimal("98.50"), closed.getActualBalance());
        assertEquals(new BigDecimal("-1.50"), closed.getDivergence());
    }

    @Test
    void shouldCreateSaleDeductStockAndRegisterNfce() {
        UUID shiftId = UUID.randomUUID();
        CashShift shift = new CashShift(
                shiftId, tenantId, companyId, cashierId, "Carlos", "CX-01",
                ShiftStatus.ABERTO, BigDecimal.ZERO, null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        CompanyJpaEntity company = CompanyJpaEntity.builder().id(companyId).tradeName("Livraria").cnpj("12.345.678/0001-95").build();
        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).productId(productId).skuCode("LIV-10").price(new BigDecimal("60.00")).build();
        ProductJpaEntity prod = ProductJpaEntity.builder().id(productId).title("O Alquimista").build();

        when(shiftRepository.findById(shiftId)).thenReturn(Optional.of(shift));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(productRepository.findById(productId)).thenReturn(Optional.of(prod));
        when(stockRepository.getAvailableStock(tenantId, companyId, skuId)).thenReturn(10);
        when(stockRepository.getPhysicalStock(tenantId, companyId, skuId)).thenReturn(10);
        when(saleRepository.save(any(PosSale.class))).thenAnswer(i -> i.getArgument(0));

        PosDto.CreateSaleRequest request = new PosDto.CreateSaleRequest(
                companyId, shiftId, null, "Maria Silva", "111.222.333-44", BigDecimal.ZERO,
                List.of(new PosDto.SaleItemRequest(skuId, 2, new BigDecimal("60.00"), BigDecimal.ZERO)),
                List.of(new PosDto.SalePaymentRequest(PaymentMethod.PIX, new BigDecimal("120.00")))
        );

        PosSale sale = posService.createSale(request, cashierId);

        assertNotNull(sale);
        assertEquals(new BigDecimal("120.00"), sale.getTotal());
        assertEquals(SaleStatus.CONCLUIDA, sale.getStatus());
        assertNotNull(sale.getNfceKey());
        assertEquals(44, sale.getNfceKey().length());
        assertNotNull(sale.getNfceXml());

        // Verifica baixa no estoque físico (10 - 2 = 8)
        verify(stockRepository).setStockQuantity(tenantId, companyId, skuId, 8);
        verify(movementRepository).save(any(StockMovement.class));
        verify(shiftRepository).save(shift);
    }

    @Test
    void shouldReturnExistingSaleIfClientUuidIsDuplicate() {
        UUID clientUuid = UUID.randomUUID();
        PosSale existing = new PosSale(
                UUID.randomUUID(), tenantId, companyId, UUID.randomUUID(), "VD-EXISTENTE", clientUuid,
                null, null, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO,
                SaleStatus.CONCLUIDA, "KEY", "XML", null,
                List.of(new PosSaleItem(UUID.randomUUID(), tenantId, UUID.randomUUID(), skuId, "SKU", "T", 1, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN)),
                List.of(new PosSalePayment(UUID.randomUUID(), tenantId, UUID.randomUUID(), PaymentMethod.PIX, BigDecimal.TEN)),
                cashierId, Instant.now(), Instant.now()
        );

        when(saleRepository.findByClientUuid(tenantId, clientUuid)).thenReturn(Optional.of(existing));

        PosDto.CreateSaleRequest request = new PosDto.CreateSaleRequest(
                companyId, null, clientUuid, null, null, BigDecimal.ZERO,
                List.of(new PosDto.SaleItemRequest(skuId, 1, BigDecimal.TEN, BigDecimal.ZERO)),
                List.of(new PosDto.SalePaymentRequest(PaymentMethod.PIX, BigDecimal.TEN))
        );

        PosSale result = posService.createSale(request, cashierId);

        assertSame(existing, result);
        verify(stockRepository, never()).setStockQuantity(any(), any(), any(), anyInt());
    }

    @Test
    void shouldCancelSaleAndRestockInventory() {
        UUID saleId = UUID.randomUUID();
        PosSaleItem item = new PosSaleItem(
                UUID.randomUUID(), tenantId, saleId, skuId, "SKU", "T", 3, new BigDecimal("30.00"), BigDecimal.ZERO, new BigDecimal("90.00")
        );
        PosSalePayment p = new PosSalePayment(UUID.randomUUID(), tenantId, saleId, PaymentMethod.DINHEIRO, new BigDecimal("90.00"));

        PosSale sale = new PosSale(
                saleId, tenantId, companyId, UUID.randomUUID(), "VD-CANCEL", null,
                null, null, new BigDecimal("90.00"), BigDecimal.ZERO, new BigDecimal("90.00"), new BigDecimal("90.00"), BigDecimal.ZERO,
                SaleStatus.CONCLUIDA, "KEY", "XML", null,
                List.of(item), List.of(p), cashierId, Instant.now(), Instant.now()
        );

        when(saleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(saleRepository.save(any(PosSale.class))).thenAnswer(i -> i.getArgument(0));
        when(stockRepository.getPhysicalStock(tenantId, companyId, skuId)).thenReturn(5);

        PosSale cancelled = posService.cancelSale(saleId, "Cliente desistiu", cashierId);

        assertEquals(SaleStatus.CANCELADA, cancelled.getStatus());
        // Estoque físico deve ser estornado de 5 para 8 (5 + 3)
        verify(stockRepository).setStockQuantity(tenantId, companyId, skuId, 8);
        verify(movementRepository).save(any(StockMovement.class));
    }

    @Test
    void shouldSyncCatalogForOfflineMode() {
        SkuJpaEntity activeSku = SkuJpaEntity.builder()
                .id(skuId).tenantId(tenantId).productId(productId).skuCode("SKU-1").barcode("789123").active(true).price(new BigDecimal("50.00")).build();
        ProductJpaEntity prod = ProductJpaEntity.builder().id(productId).title("Livro Offline").build();

        when(skuRepository.findAll()).thenReturn(List.of(activeSku));
        when(productRepository.findById(productId)).thenReturn(Optional.of(prod));
        when(stockRepository.getPhysicalStock(tenantId, companyId, skuId)).thenReturn(25);
        when(stockRepository.getAvailableStock(tenantId, companyId, skuId)).thenReturn(20);

        PosDto.OfflineCatalogSyncResponse sync = posService.syncCatalog(companyId, null);

        assertNotNull(sync);
        assertEquals(1, sync.totalItems());
        assertEquals("SKU-1", sync.items().get(0).skuCode());
        assertEquals("Livro Offline", sync.items().get(0).title());
        assertEquals(25, sync.items().get(0).physicalStock());
        assertEquals(20, sync.items().get(0).availableStock());
    }

    @Test
    void shouldBatchSyncOfflineSalesHandlingDuplicates() {
        UUID clientUuid1 = UUID.randomUUID();
        UUID clientUuid2 = UUID.randomUUID();

        // clientUuid1 já foi sincronizada anteriormente
        when(saleRepository.findByClientUuid(tenantId, clientUuid1)).thenReturn(Optional.of(mock(PosSale.class)));

        // clientUuid2 é nova: preparamos mocks para createSale
        when(saleRepository.findByClientUuid(tenantId, clientUuid2)).thenReturn(Optional.empty());
        CashShift shift = new CashShift(
                UUID.randomUUID(), tenantId, companyId, cashierId, "Carlos", "CX-01",
                ShiftStatus.ABERTO, BigDecimal.ZERO, null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );
        when(shiftRepository.findCurrentOpenShift(tenantId, companyId, cashierId)).thenReturn(Optional.of(shift));
        CompanyJpaEntity company = CompanyJpaEntity.builder().id(companyId).tradeName("Livraria").build();
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).productId(productId).skuCode("SKU-2").price(new BigDecimal("30.00")).build();
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(productRepository.findById(productId)).thenReturn(Optional.of(ProductJpaEntity.builder().title("T").build()));
        when(stockRepository.getAvailableStock(tenantId, companyId, skuId)).thenReturn(10);
        when(stockRepository.getPhysicalStock(tenantId, companyId, skuId)).thenReturn(10);
        when(saleRepository.save(any(PosSale.class))).thenAnswer(i -> i.getArgument(0));

        PosDto.CreateSaleRequest s1 = new PosDto.CreateSaleRequest(companyId, null, clientUuid1, null, null, BigDecimal.ZERO, List.of(), List.of());
        PosDto.CreateSaleRequest s2 = new PosDto.CreateSaleRequest(
                companyId, null, clientUuid2, null, null, BigDecimal.ZERO,
                List.of(new PosDto.SaleItemRequest(skuId, 1, new BigDecimal("30.00"), BigDecimal.ZERO)),
                List.of(new PosDto.SalePaymentRequest(PaymentMethod.DINHEIRO, new BigDecimal("30.00")))
        );

        PosDto.OfflineBatchSyncResult result = posService.syncOfflineSales(List.of(s1, s2), cashierId);

        assertEquals(2, result.totalReceived());
        assertEquals(1, result.successfullyProcessed());
        assertEquals(1, result.alreadyProcessedDuplicates());
        assertEquals(0, result.failures());
    }
}
