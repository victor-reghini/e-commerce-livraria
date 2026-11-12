package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.application.dto.fiscal.FiscalDto;
import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import com.livraria.ecommerce.domain.model.fiscal.SefazStatusResult;
import com.livraria.ecommerce.domain.port.in.FiscalUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - FiscalController (Endpoints SEFAZ NF-e e NFC-e)")
class FiscalControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FiscalUseCase fiscalUseCase;

    private FiscalController fiscalController;

    @BeforeEach
    void setUp() {
        fiscalController = new FiscalController(fiscalUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(fiscalController).build();
    }

    @Test
    @DisplayName("POST /api/v1/fiscal/invoices/nfe/order/{orderId} - Deve emitir NF-e 55 e retornar 201 Created")
    void shouldEmitNfeForOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        FiscalInvoice invoice = FiscalInvoice.createForOrder(
                UUID.randomUUID(), UUID.randomUUID(), orderId, 1, 101L,
                "35261112345678000190550010000001011123456781", new BigDecimal("100.00"), BigDecimal.ZERO, "VENDA"
        );
        invoice.setId(UUID.randomUUID());
        invoice.authorize("135260001234567", "<xml></xml>");

        when(fiscalUseCase.emitNfeForOrder(eq(orderId), anyInt())).thenReturn(invoice);

        mockMvc.perform(post("/api/v1/fiscal/invoices/nfe/order/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"series\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(invoice.getId().toString()))
                .andExpect(jsonPath("$.model").value("NFE_55"))
                .andExpect(jsonPath("$.status").value("AUTORIZADA"))
                .andExpect(jsonPath("$.protocolNumber").value("135260001234567"))
                .andExpect(jsonPath("$.accessKey").value("35261112345678000190550010000001011123456781"));

        verify(fiscalUseCase).emitNfeForOrder(orderId, 1);
    }

    @Test
    @DisplayName("POST /api/v1/fiscal/invoices/nfce/sale/{saleId} - Deve emitir NFC-e 65 e retornar 201 Created")
    void shouldEmitNfceForSale() throws Exception {
        UUID saleId = UUID.randomUUID();
        FiscalInvoice invoice = FiscalInvoice.createForPosSale(
                UUID.randomUUID(), UUID.randomUUID(), saleId, 1, 201L,
                "35261112345678000190650010000002011123456781", new BigDecimal("45.00"), BigDecimal.ZERO
        );
        invoice.setId(UUID.randomUUID());
        invoice.authorize("135260009876543", "<xml></xml>");

        when(fiscalUseCase.emitNfceForSale(eq(saleId), anyInt())).thenReturn(invoice);

        mockMvc.perform(post("/api/v1/fiscal/invoices/nfce/sale/" + saleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"series\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.model").value("NFCE_65"))
                .andExpect(jsonPath("$.status").value("AUTORIZADA"))
                .andExpect(jsonPath("$.protocolNumber").value("135260009876543"));

        verify(fiscalUseCase).emitNfceForSale(saleId, 1);
    }

    @Test
    @DisplayName("GET /api/v1/fiscal/invoices/{id}/xml - Deve retornar XML com Content-Type application/xml")
    void shouldDownloadXml() throws Exception {
        UUID id = UUID.randomUUID();
        String sampleXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><nfeProc><NFe></NFe></nfeProc>";

        when(fiscalUseCase.getInvoiceXml(id)).thenReturn(sampleXml);

        mockMvc.perform(get("/api/v1/fiscal/invoices/" + id + "/xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"nfe-" + id + ".xml\""))
                .andExpect(content().string(sampleXml));
    }

    @Test
    @DisplayName("GET /api/v1/fiscal/invoices/{id}/danfe - Deve retornar HTML com Content-Type text/html")
    void shouldPreviewDanfeHtml() throws Exception {
        UUID id = UUID.randomUUID();
        String danfeHtml = "<!DOCTYPE html><html><body>DANFE 55</body></html>";

        when(fiscalUseCase.generateDanfeMirror(id)).thenReturn(danfeHtml);

        mockMvc.perform(get("/api/v1/fiscal/invoices/" + id + "/danfe"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML_VALUE))
                .andExpect(content().string(danfeHtml));
    }

    @Test
    @DisplayName("POST /api/v1/fiscal/invoices/{id}/cancel - Deve cancelar nota fiscal e retornar 200 OK")
    void shouldCancelInvoice() throws Exception {
        UUID id = UUID.randomUUID();
        FiscalInvoice invoice = FiscalInvoice.createForOrder(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, 100L,
                "key", BigDecimal.TEN, BigDecimal.ZERO, "VENDA"
        );
        invoice.setId(id);
        invoice.authorize("prot1", "<xml></xml>");
        invoice.cancel("prot2", "Cancelamento solicitado pelo cliente via suporte");

        when(fiscalUseCase.cancelInvoice(eq(id), anyString())).thenReturn(invoice);

        FiscalDto.CancelInvoiceRequest req = new FiscalDto.CancelInvoiceRequest("Cancelamento solicitado pelo cliente via suporte");

        mockMvc.perform(post("/api/v1/fiscal/invoices/" + id + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.cancellationReason").value("Cancelamento solicitado pelo cliente via suporte"));
    }

    @Test
    @DisplayName("GET /api/v1/fiscal/status-sefaz - Deve consultar status do WebService SEFAZ")
    void shouldConsultSefazStatus() throws Exception {
        when(fiscalUseCase.consultSefazStatus("SP"))
                .thenReturn(SefazStatusResult.operational("SP", "homologation", 42L));

        mockMvc.perform(get("/api/v1/fiscal/status-sefaz?uf=SP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operational").value(true))
                .andExpect(jsonPath("$.statusCode").value(107))
                .andExpect(jsonPath("$.statusMessage").value("Servico em Operacao"))
                .andExpect(jsonPath("$.uf").value("SP"));
    }
}
