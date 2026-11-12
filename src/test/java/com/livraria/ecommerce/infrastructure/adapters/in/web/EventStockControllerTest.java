package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.application.dto.stock.EventStockDto;
import com.livraria.ecommerce.domain.model.stock.AuditStatus;
import com.livraria.ecommerce.domain.model.stock.BlindAudit;
import com.livraria.ecommerce.domain.model.stock.EventDashboard;
import com.livraria.ecommerce.domain.model.stock.MovementType;
import com.livraria.ecommerce.domain.model.stock.StockMovement;
import com.livraria.ecommerce.domain.model.stock.StockTransfer;
import com.livraria.ecommerce.domain.model.stock.StockTransferItem;
import com.livraria.ecommerce.domain.model.stock.TransferStatus;
import com.livraria.ecommerce.domain.model.stock.TransferType;
import com.livraria.ecommerce.domain.port.in.EventStockUseCase;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - EventStockController")
class EventStockControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EventStockUseCase eventStockUseCase;

    @InjectMocks
    private EventStockController eventStockController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID originId = UUID.randomUUID();
    private final UUID destId = UUID.randomUUID();
    private final UUID transferId = UUID.randomUUID();
    private final UUID auditId = UUID.randomUUID();
    private final UUID skuId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventStockController).build();
    }

    @Test
    void shouldCreateTransferEndpoint() throws Exception {
        StockTransfer transfer = new StockTransfer(
                transferId, tenantId, "TRF-001", originId, "Origem", destId, "Destino",
                TransferStatus.RESERVADO, TransferType.REMESSA_EVENTO, "Feira SP",
                Collections.emptyList(), null, null, null, null, Instant.now(), Instant.now()
        );

        when(eventStockUseCase.createTransfer(any(), any())).thenReturn(transfer);

        EventStockDto.CreateTransferRequest request = new EventStockDto.CreateTransferRequest(
                originId, destId, TransferType.REMESSA_EVENTO, "Feira SP",
                List.of(new EventStockDto.TransferItemRequest(skuId, 5, new BigDecimal("49.90")))
        );

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferNumber").value("TRF-001"))
                .andExpect(jsonPath("$.status").value("RESERVADO"));
    }

    @Test
    void shouldGetTransferByIdEndpoint() throws Exception {
        StockTransfer transfer = new StockTransfer(
                transferId, tenantId, "TRF-001", originId, "Origem", destId, "Destino",
                TransferStatus.RESERVADO, TransferType.REMESSA_EVENTO, null,
                Collections.emptyList(), null, null, null, null, Instant.now(), Instant.now()
        );

        when(eventStockUseCase.getTransferById(transferId)).thenReturn(transfer);

        mockMvc.perform(get("/api/v1/transfers/" + transferId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferNumber").value("TRF-001"));
    }

    @Test
    void shouldDispatchTransferEndpoint() throws Exception {
        StockTransfer transfer = new StockTransfer(
                transferId, tenantId, "TRF-001", originId, "Origem", destId, "Destino",
                TransferStatus.EM_TRANSITO, TransferType.REMESSA_EVENTO, null,
                Collections.emptyList(), Instant.now(), null, null, null, Instant.now(), Instant.now()
        );

        when(eventStockUseCase.dispatchTransfer(eq(transferId), any())).thenReturn(transfer);

        mockMvc.perform(post("/api/v1/transfers/" + transferId + "/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_TRANSITO"));
    }

    @Test
    void shouldReceiveTransferEndpoint() throws Exception {
        StockTransfer transfer = new StockTransfer(
                transferId, tenantId, "TRF-001", originId, "Origem", destId, "Destino",
                TransferStatus.RECEBIDO, TransferType.REMESSA_EVENTO, null,
                Collections.emptyList(), Instant.now(), Instant.now(), null, null, Instant.now(), Instant.now()
        );

        when(eventStockUseCase.receiveTransfer(eq(transferId), any(), any())).thenReturn(transfer);

        EventStockDto.ReceiveTransferRequest req = new EventStockDto.ReceiveTransferRequest(Map.of(skuId, 10));

        mockMvc.perform(post("/api/v1/transfers/" + transferId + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEBIDO"));
    }

    @Test
    void shouldGetRomaneioEndpoint() throws Exception {
        EventStockDto.RomaneioDocumentResponse romaneio = new EventStockDto.RomaneioDocumentResponse(
                "TRF-001", "Matriz", "Feira", TransferType.REMESSA_EVENTO, Instant.now(),
                Collections.emptyList(), 50, 15000, new BigDecimal("2500.00"), "Romaneio ok"
        );

        when(eventStockUseCase.generateRomaneio(transferId)).thenReturn(romaneio);

        mockMvc.perform(get("/api/v1/transfers/" + transferId + "/romaneio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferNumber").value("TRF-001"))
                .andExpect(jsonPath("$.quantidadeTotalLivros").value(50))
                .andExpect(jsonPath("$.pesoTotalGramas").value(15000));
    }

    @Test
    void shouldCreateBlindAuditEndpoint() throws Exception {
        BlindAudit audit = new BlindAudit(
                auditId, tenantId, destId, "Feira", null, "AUD-100",
                AuditStatus.ABERTA, null, "Auditoria pós evento", Collections.emptyList(), Instant.now(), null
        );

        when(eventStockUseCase.createBlindAudit(any(), any())).thenReturn(audit);

        EventStockDto.CreateAuditRequest req = new EventStockDto.CreateAuditRequest(destId, null, "Auditoria pós evento");

        mockMvc.perform(post("/api/v1/audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("AUD-100"))
                .andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    void shouldScanBarcodeEndpoint() throws Exception {
        BlindAudit audit = new BlindAudit(
                auditId, tenantId, destId, "Feira", null, "AUD-100",
                AuditStatus.EM_BIPAGEM, null, null, Collections.emptyList(), Instant.now(), null
        );

        when(eventStockUseCase.scanBarcode(auditId, "7891234567890", 2)).thenReturn(audit);

        EventStockDto.ScanBarcodeRequest req = new EventStockDto.ScanBarcodeRequest("7891234567890", 2);

        mockMvc.perform(post("/api/v1/audits/" + auditId + "/scan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_BIPAGEM"));
    }

    @Test
    void shouldFinalizeAndReconcileAuditEndpoints() throws Exception {
        BlindAudit auditFinal = new BlindAudit(
                auditId, tenantId, destId, "Feira", null, "AUD-100",
                AuditStatus.FINALIZADA, null, null, Collections.emptyList(), Instant.now(), Instant.now()
        );
        BlindAudit auditReconciled = new BlindAudit(
                auditId, tenantId, destId, "Feira", null, "AUD-100",
                AuditStatus.CONCILIADA, null, null, Collections.emptyList(), Instant.now(), Instant.now()
        );

        when(eventStockUseCase.finalizeAudit(auditId)).thenReturn(auditFinal);
        when(eventStockUseCase.reconcileAudit(eq(auditId), any())).thenReturn(auditReconciled);

        mockMvc.perform(post("/api/v1/audits/" + auditId + "/finalize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADA"));

        mockMvc.perform(post("/api/v1/audits/" + auditId + "/reconcile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCILIADA"));
    }

    @Test
    void shouldGetEventDashboardEndpoint() throws Exception {
        EventDashboard dashboard = new EventDashboard(
                destId, "Feira do Livro", 200, new BigDecimal("8000.00"),
                120, new BigDecimal("4800.00"), 80, 0, Collections.emptyList(), Collections.emptyList()
        );

        when(eventStockUseCase.getEventDashboard(destId)).thenReturn(dashboard);

        mockMvc.perform(get("/api/v1/events/" + destId + "/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Feira do Livro"))
                .andExpect(jsonPath("$.totalItemsDispatched").value(200))
                .andExpect(jsonPath("$.totalItemsSold").value(120))
                .andExpect(jsonPath("$.currentPhysicalStock").value(80));
    }
}

