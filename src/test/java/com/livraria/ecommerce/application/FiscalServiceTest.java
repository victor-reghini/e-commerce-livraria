package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.service.FiscalService;
import com.livraria.ecommerce.application.service.ObservabilityMetricsService;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.company.Address;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderItem;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingAddress;
import com.livraria.ecommerce.domain.model.fiscal.FiscalCancellationResult;
import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import com.livraria.ecommerce.domain.model.fiscal.FiscalModel;
import com.livraria.ecommerce.domain.model.fiscal.FiscalStatus;
import com.livraria.ecommerce.domain.model.fiscal.SefazAuthResult;
import com.livraria.ecommerce.domain.model.fiscal.SefazStatusResult;
import com.livraria.ecommerce.domain.model.pos.NfceStatus;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.PosSalePayment;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import com.livraria.ecommerce.domain.port.out.CompanyRepositoryPort;
import com.livraria.ecommerce.domain.port.out.FiscalRepositoryPort;
import com.livraria.ecommerce.domain.port.out.OrderRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PosSaleRepositoryPort;
import com.livraria.ecommerce.domain.port.out.SefazGatewayPort;
import com.livraria.ecommerce.infrastructure.adapters.out.fiscal.DanfeHtmlGenerator;
import com.livraria.ecommerce.infrastructure.adapters.out.fiscal.NfeXmlBuilder;
import com.livraria.ecommerce.infrastructure.adapters.out.fiscal.SefazAccessKeyGenerator;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - FiscalService (Homologação Fiscal SEFAZ NF-e 55 e NFC-e 65)")
class FiscalServiceTest {

    @Mock
    private FiscalRepositoryPort fiscalRepository;

    @Mock
    private SefazGatewayPort sefazGateway;

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private PosSaleRepositoryPort posSaleRepository;

    @Mock
    private CompanyRepositoryPort companyRepository;

    @Mock
    private AuditUseCase auditUseCase;

    @Mock
    private ObservabilityMetricsService metricsService;

    private SefazAccessKeyGenerator accessKeyGenerator;
    private NfeXmlBuilder nfeXmlBuilder;
    private DanfeHtmlGenerator danfeHtmlGenerator;
    private FiscalService fiscalService;

    private UUID tenantId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        accessKeyGenerator = new SefazAccessKeyGenerator();
        nfeXmlBuilder = new NfeXmlBuilder();
        danfeHtmlGenerator = new DanfeHtmlGenerator();

        fiscalService = new FiscalService(
                fiscalRepository,
                sefazGateway,
                accessKeyGenerator,
                nfeXmlBuilder,
                danfeHtmlGenerator,
                orderRepository,
                posSaleRepository,
                companyRepository,
                auditUseCase,
                metricsService
        );

        tenantId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve emitir e autorizar NF-e Modelo 55 para um pedido de e-commerce com sucesso")
    void shouldEmitAndAuthorizeNfeForOrder() {
        UUID orderId = UUID.randomUUID();

        OrderItem item = new OrderItem(
                UUID.randomUUID(), UUID.randomUUID(), "LIV-01", "Livro Clean Architecture",
                2, new BigDecimal("60.00"), BigDecimal.ZERO, new BigDecimal("120.00"), "49019900", "5102"
        );

        ShippingAddress address = new ShippingAddress(
                "Av. Paulista", "1500", "Apto 101", "Bela Vista", "São Paulo", "SP", "01310100"
        );

        Order order = new Order(
                orderId, tenantId, "ORD-2026-0001", UUID.randomUUID(),
                "Carlos Silva", "carlos@email.com", "12345678901", "11988887777",
                companyId, OrderStatus.PAGO, new BigDecimal("120.00"), BigDecimal.ZERO,
                new BigDecimal("15.00"), new BigDecimal("135.00"), null, null, null, null,
                address, List.of(item), new ArrayList<>(), null,
                Instant.now(), Instant.now(), Instant.now(), null, null, null
        );

        Company company = Company.create(
                tenantId, "MATRIZ", "Livraria Matriz", "Livraria", "12.345.678/0001-90",
                "123456789", null, null, true,
                new Address("Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP", "01310100")
        );

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(fiscalRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(fiscalRepository.getNextInvoiceNumber(eq(tenantId), eq(company.getId()), eq("55"), eq(1)))
                .thenReturn(101L);

        when(sefazGateway.authorizeInvoice(anyString(), anyString(), eq("55"), anyString()))
                .thenReturn(SefazAuthResult.success("135260001234567", "35261112345678000190550010000001011123456781", "digest==", "<xml>authorized</xml>"));

        when(fiscalRepository.save(any(FiscalInvoice.class))).thenAnswer(i -> i.getArgument(0));

        FiscalInvoice invoice = fiscalService.emitNfeForOrder(orderId, 1);

        assertNotNull(invoice);
        assertEquals(FiscalModel.NFE_55, invoice.getModel());
        assertEquals(FiscalStatus.AUTORIZADA, invoice.getStatus());
        assertEquals("135260001234567", invoice.getProtocolNumber());
        assertEquals(44, invoice.getAccessKey().length());
        assertTrue(invoice.getAccessKey().matches("\\d{44}"));

        verify(metricsService).recordInvoiceEmitted("55", "AUTORIZADA");
        verify(auditUseCase).recordEvent(any(), any(), any(), any(), eq("FISCAL_NFE_EMITTED"), any(), any(), any());
    }

    @Test
    @DisplayName("Deve retornar NF-e já autorizada de forma idempotente sem duplicar emissão")
    void shouldReturnExistingAuthorizedInvoiceIdempotently() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order(
                orderId, tenantId, "ORD-1", UUID.randomUUID(),
                "Cliente", "c@c.com", "11122233344", "1199999",
                companyId, OrderStatus.PAGO, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN,
                null, null, null, null, null, List.of(), List.of(), null,
                Instant.now(), Instant.now(), Instant.now(), null, null, null
        );

        FiscalInvoice existing = FiscalInvoice.createForOrder(
                tenantId, companyId, orderId, 1, 50L, "key44", BigDecimal.TEN, BigDecimal.ZERO, "VENDA"
        );
        existing.authorize("135260000000001", "<xml></xml>");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(fiscalRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        FiscalInvoice result = fiscalService.emitNfeForOrder(orderId, 1);

        assertSame(existing, result);
        verify(sefazGateway, never()).authorizeInvoice(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve emitir e autorizar NFC-e Modelo 65 para Venda PDV Balcão")
    void shouldEmitAndAuthorizeNfceForPosSale() {
        UUID saleId = UUID.randomUUID();
        PosSaleItem item = new PosSaleItem(
                UUID.randomUUID(), tenantId, saleId, UUID.randomUUID(), "LIV-02", "O Alquimista",
                1, new BigDecimal("39.90"), BigDecimal.ZERO, new BigDecimal("39.90")
        );
        PosSalePayment payment = new PosSalePayment(
                UUID.randomUUID(), tenantId, saleId, PaymentMethod.DINHEIRO, new BigDecimal("40.00")
        );

        PosSale sale = new PosSale(
                saleId, tenantId, companyId, UUID.randomUUID(), "VD-0001", UUID.randomUUID(),
                "Cliente Balcao", "11122233344", new BigDecimal("39.90"), BigDecimal.ZERO,
                new BigDecimal("39.90"), new BigDecimal("40.00"), new BigDecimal("0.10"),
                SaleStatus.CONCLUIDA, null, null, NfceStatus.EMITIDA,
                List.of(item), List.of(payment), UUID.randomUUID(), Instant.now(), Instant.now()
        );

        Company company = Company.create(
                tenantId, "FILIAL-01", "Livraria Filial", "Filial", "12.345.678/0002-71",
                "987654321", null, null, false,
                new Address("Rua Augusta", "500", null, "Consolação", "São Paulo", "SP", "01305100")
        );

        when(posSaleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(fiscalRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(fiscalRepository.getNextInvoiceNumber(eq(tenantId), eq(company.getId()), eq("65"), eq(1)))
                .thenReturn(205L);

        when(sefazGateway.authorizeInvoice(anyString(), anyString(), eq("65"), anyString()))
                .thenReturn(SefazAuthResult.success("135260009876543", "35261112345678000271650010000002051123456781", "digest==", "<xml>nfce</xml>"));

        when(fiscalRepository.save(any(FiscalInvoice.class))).thenAnswer(i -> i.getArgument(0));

        FiscalInvoice invoice = fiscalService.emitNfceForSale(saleId, 1);

        assertNotNull(invoice);
        assertEquals(FiscalModel.NFCE_65, invoice.getModel());
        assertEquals(FiscalStatus.AUTORIZADA, invoice.getStatus());
        assertEquals("135260009876543", invoice.getProtocolNumber());

        verify(posSaleRepository).save(sale);
        verify(metricsService).recordInvoiceEmitted("65", "AUTORIZADA");
    }

    @Test
    @DisplayName("Deve emitir em contingência offline quando a SEFAZ estiver indisponível")
    void shouldEmitInContingencyWhenSefazIsUnavailable() {
        UUID saleId = UUID.randomUUID();
        PosSaleItem item = new PosSaleItem(
                UUID.randomUUID(), tenantId, saleId, UUID.randomUUID(), "LIV-02", "O Alquimista",
                1, new BigDecimal("39.90"), BigDecimal.ZERO, new BigDecimal("39.90")
        );
        PosSalePayment payment = new PosSalePayment(
                UUID.randomUUID(), tenantId, saleId, PaymentMethod.DINHEIRO, new BigDecimal("39.90")
        );
        PosSale sale = new PosSale(
                saleId, tenantId, companyId, UUID.randomUUID(), "VD-0002", UUID.randomUUID(),
                "Cliente", null, new BigDecimal("39.90"), BigDecimal.ZERO,
                new BigDecimal("39.90"), new BigDecimal("39.90"), BigDecimal.ZERO,
                SaleStatus.CONCLUIDA, null, null, NfceStatus.EMITIDA,
                List.of(item), List.of(payment), UUID.randomUUID(), Instant.now(), Instant.now()
        );

        Company company = Company.create(
                tenantId, "MATRIZ", "Livraria", "Livraria", "12.345.678/0001-90",
                "123", null, null, true, null
        );

        when(posSaleRepository.findById(saleId)).thenReturn(Optional.of(sale));
        when(fiscalRepository.findBySaleId(saleId)).thenReturn(Optional.empty());
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(fiscalRepository.getNextInvoiceNumber(any(), any(), any(), anyInt())).thenReturn(206L);

        when(sefazGateway.authorizeInvoice(anyString(), anyString(), eq("65"), anyString()))
                .thenReturn(SefazAuthResult.contingency("accessKey", "Manutenção SEFAZ", "<xml>offline</xml>"));
        when(fiscalRepository.save(any(FiscalInvoice.class))).thenAnswer(i -> i.getArgument(0));

        FiscalInvoice invoice = fiscalService.emitNfceForSale(saleId, 1);

        assertEquals(FiscalStatus.CONTINGENCIA, invoice.getStatus());
        assertEquals(NfceStatus.CONTINGENCIA_OFFLINE, sale.getNfceStatus());
        verify(metricsService).recordInvoiceEmitted("65", "CONTINGENCIA");
    }

    @Test
    @DisplayName("Deve cancelar NF-e autorizada com evento de cancelamento registrado na SEFAZ")
    void shouldCancelAuthorizedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        FiscalInvoice invoice = FiscalInvoice.createForOrder(
                tenantId, companyId, UUID.randomUUID(), 1, 100L,
                "35261112345678000190550010000001001123456781", BigDecimal.TEN, BigDecimal.ZERO, "VENDA"
        );
        invoice.setId(invoiceId);
        invoice.authorize("135260001234567", "<xml></xml>");

        when(fiscalRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());
        when(sefazGateway.cancelInvoice(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(FiscalCancellationResult.success("135260009999999", invoice.getAccessKey()));
        when(fiscalRepository.save(any(FiscalInvoice.class))).thenAnswer(i -> i.getArgument(0));

        FiscalInvoice canceled = fiscalService.cancelInvoice(invoiceId, "Cancelamento solicitado pelo cliente via atendimento");

        assertNotNull(canceled);
        assertEquals(FiscalStatus.CANCELADA, canceled.getStatus());
        assertEquals("135260009999999", canceled.getProtocolNumber());

        verify(metricsService).incrementInvoicesCanceled();
        verify(auditUseCase).recordEvent(any(), any(), any(), any(), eq("FISCAL_INVOICE_CANCELLED"), any(), any(), any());
    }

    @Test
    @DisplayName("Deve consultar status do serviço SEFAZ")
    void shouldConsultSefazStatus() {
        when(sefazGateway.checkServiceStatus("SP", null))
                .thenReturn(SefazStatusResult.operational("SP", "homologation", 50L));

        SefazStatusResult status = fiscalService.consultSefazStatus("SP");

        assertTrue(status.operational());
        assertEquals(107, status.statusCode());
        assertEquals("Servico em Operacao", status.statusMessage());
    }

    @Test
    @DisplayName("Deve gerar espelho HTML do DANFE com dados do emitente e imunidade tributária")
    void shouldGenerateDanfeMirror() {
        UUID invoiceId = UUID.randomUUID();
        FiscalInvoice invoice = FiscalInvoice.createForOrder(
                tenantId, companyId, UUID.randomUUID(), 1, 100L,
                "35261112345678000190550010000001001123456781", new BigDecimal("89.90"), BigDecimal.ZERO, "VENDA"
        );
        invoice.setId(invoiceId);
        invoice.authorize("135260001234567", "<xml></xml>");

        when(fiscalRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(companyRepository.findById(companyId)).thenReturn(Optional.empty());

        String html = fiscalService.generateDanfeMirror(invoiceId);

        assertNotNull(html);
        assertTrue(html.contains("DANFE"));
        assertTrue(html.contains("3526 1112 3456 7800 0190 5500 1000 0001 0011 2345 6781"));
        assertTrue(html.contains("IMUNIDADE TRIBUTÁRIA"));
    }
}
