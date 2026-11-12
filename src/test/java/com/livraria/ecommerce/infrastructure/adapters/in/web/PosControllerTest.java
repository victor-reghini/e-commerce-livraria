package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.application.dto.pos.PosDto;
import com.livraria.ecommerce.domain.model.pos.CashShift;
import com.livraria.ecommerce.domain.model.pos.NfceStatus;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.PosSalePayment;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import com.livraria.ecommerce.domain.model.pos.ShiftStatus;
import com.livraria.ecommerce.domain.port.in.PosUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - PosController (Endpoints PDV, Caixa e Venda)")
class PosControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PosUseCase posUseCase;

    @InjectMocks
    private PosController posController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID cashierId = UUID.randomUUID();
    private final UUID shiftId = UUID.randomUUID();
    private final UUID saleId = UUID.randomUUID();
    private final UUID skuId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(posController).build();
    }

    @Test
    void shouldOpenShiftEndpoint() throws Exception {
        CashShift shift = new CashShift(
                shiftId, tenantId, companyId, cashierId, "Carlos Balcão", "CX-100",
                ShiftStatus.ABERTO, new BigDecimal("100.00"), null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        when(posUseCase.openShift(eq(companyId), any(), any())).thenReturn(shift);

        PosDto.OpenShiftRequest request = new PosDto.OpenShiftRequest(companyId, new BigDecimal("100.00"));

        mockMvc.perform(post("/api/v1/pos/shifts/open")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shiftNumber").value("CX-100"))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.openingBalance").value(100.00));
    }

    @Test
    void shouldSupplyCashEndpoint() throws Exception {
        CashShift shift = new CashShift(
                shiftId, tenantId, companyId, cashierId, "Carlos", "CX-100",
                ShiftStatus.ABERTO, new BigDecimal("100.00"), null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        when(posUseCase.supplyCash(eq(shiftId), any(), any(), any())).thenReturn(shift);

        PosDto.CashMovementRequest req = new PosDto.CashMovementRequest(new BigDecimal("50.00"), "Troco");

        mockMvc.perform(post("/api/v1/pos/shifts/" + shiftId + "/supply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void shouldBleedCashEndpoint() throws Exception {
        CashShift shift = new CashShift(
                shiftId, tenantId, companyId, cashierId, "Carlos", "CX-100",
                ShiftStatus.ABERTO, new BigDecimal("100.00"), null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        when(posUseCase.bleedCash(eq(shiftId), any(), any(), any())).thenReturn(shift);

        PosDto.CashMovementRequest req = new PosDto.CashMovementRequest(new BigDecimal("30.00"), "Sangria");

        mockMvc.perform(post("/api/v1/pos/shifts/" + shiftId + "/bleed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void shouldCloseShiftEndpoint() throws Exception {
        CashShift shift = new CashShift(
                shiftId, tenantId, companyId, cashierId, "Carlos", "CX-100",
                ShiftStatus.FECHADO, new BigDecimal("100.00"), new BigDecimal("100.00"),
                new BigDecimal("99.00"), new BigDecimal("-1.00"), null,
                new ArrayList<>(), Instant.now(), Instant.now(), Instant.now(), Instant.now()
        );

        when(posUseCase.closeShift(eq(shiftId), any(), any(), any())).thenReturn(shift);

        PosDto.CloseShiftRequest req = new PosDto.CloseShiftRequest(new BigDecimal("99.00"), "Fechamento");

        mockMvc.perform(post("/api/v1/pos/shifts/" + shiftId + "/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FECHADO"))
                .andExpect(jsonPath("$.actualBalance").value(99.00))
                .andExpect(jsonPath("$.divergence").value(-1.00));
    }

    @Test
    void shouldGetCurrentShiftEndpoint() throws Exception {
        CashShift shift = new CashShift(
                shiftId, tenantId, companyId, cashierId, "Carlos", "CX-100",
                ShiftStatus.ABERTO, new BigDecimal("100.00"), null, null, null, null,
                new ArrayList<>(), Instant.now(), null, Instant.now(), Instant.now()
        );

        when(posUseCase.getCurrentOpenShift(eq(companyId), any())).thenReturn(Optional.of(shift));

        mockMvc.perform(get("/api/v1/pos/shifts/current?companyId=" + companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftNumber").value("CX-100"));
    }

    @Test
    void shouldCreateSaleEndpoint() throws Exception {
        PosSaleItem item = new PosSaleItem(
                UUID.randomUUID(), tenantId, saleId, skuId, "SKU-01", "Livro Teste",
                1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00")
        );
        PosSalePayment payment = new PosSalePayment(
                UUID.randomUUID(), tenantId, saleId, PaymentMethod.PIX, new BigDecimal("50.00")
        );

        PosSale sale = new PosSale(
                saleId, tenantId, companyId, shiftId, "VD-999", null,
                "Cliente Teste", "12345678901", new BigDecimal("50.00"), BigDecimal.ZERO,
                new BigDecimal("50.00"), new BigDecimal("50.00"), BigDecimal.ZERO,
                SaleStatus.CONCLUIDA, "35261112345678000195650010000000011123456789", null,
                NfceStatus.EMITIDA, List.of(item), List.of(payment), cashierId, Instant.now(), Instant.now()
        );

        when(posUseCase.createSale(any(), any())).thenReturn(sale);

        PosDto.CreateSaleRequest request = new PosDto.CreateSaleRequest(
                companyId, shiftId, null, "Cliente Teste", "12345678901", BigDecimal.ZERO,
                List.of(new PosDto.SaleItemRequest(skuId, 1, new BigDecimal("50.00"), BigDecimal.ZERO)),
                List.of(new PosDto.SalePaymentRequest(PaymentMethod.PIX, new BigDecimal("50.00")))
        );

        mockMvc.perform(post("/api/v1/pos/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.saleNumber").value("VD-999"))
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.nfceStatus").value("EMITIDA"));
    }

    @Test
    void shouldCancelSaleEndpoint() throws Exception {
        PosSale sale = new PosSale(
                saleId, tenantId, companyId, shiftId, "VD-999", null,
                null, null, new BigDecimal("50.00"), BigDecimal.ZERO,
                new BigDecimal("50.00"), new BigDecimal("50.00"), BigDecimal.ZERO,
                SaleStatus.CANCELADA, null, null, NfceStatus.CANCELADA,
                List.of(new PosSaleItem(UUID.randomUUID(), tenantId, saleId, skuId, "S", "T", 1, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN)),
                List.of(new PosSalePayment(UUID.randomUUID(), tenantId, saleId, PaymentMethod.PIX, BigDecimal.TEN)),
                cashierId, Instant.now(), Instant.now()
        );

        when(posUseCase.cancelSale(eq(saleId), any(), any())).thenReturn(sale);

        mockMvc.perform(post("/api/v1/pos/sales/" + saleId + "/cancel?reason=Desistencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    void shouldGetThermalReceiptEndpoint() throws Exception {
        PosDto.ThermalReceiptResponse receipt = new PosDto.ThermalReceiptResponse(
                "VD-999", "Livraria Cultura", "12.345.678/0001-95", "Rua das Letras, 100",
                Instant.now(), Collections.emptyList(), new BigDecimal("100.00"), BigDecimal.ZERO,
                new BigDecimal("100.00"), Collections.emptyList(), BigDecimal.ZERO,
                "352611KEY44", "https://sefaz.sp.gov.br", "Operador 1"
        );

        when(posUseCase.getThermalReceipt(saleId)).thenReturn(receipt);

        mockMvc.perform(get("/api/v1/pos/sales/" + saleId + "/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saleNumber").value("VD-999"))
                .andExpect(jsonPath("$.companyName").value("Livraria Cultura"))
                .andExpect(jsonPath("$.total").value(100.00));
    }

    @Test
    void shouldSyncCatalogEndpoint() throws Exception {
        PosDto.OfflineCatalogSyncResponse sync = new PosDto.OfflineCatalogSyncResponse(
                companyId, Instant.now(), 10, Collections.emptyList()
        );

        when(posUseCase.syncCatalog(eq(companyId), any())).thenReturn(sync);

        mockMvc.perform(get("/api/v1/pos/sync/catalog?companyId=" + companyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(10));
    }

    @Test
    void shouldSyncOfflineSalesEndpoint() throws Exception {
        PosDto.OfflineBatchSyncResult result = new PosDto.OfflineBatchSyncResult(
                3, 2, 1, 0, Collections.emptyList()
        );

        when(posUseCase.syncOfflineSales(any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/pos/sync/offline-sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReceived").value(3))
                .andExpect(jsonPath("$.successfullyProcessed").value(2))
                .andExpect(jsonPath("$.alreadyProcessedDuplicates").value(1));
    }
}
