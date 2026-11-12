package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.domain.port.in.EcommerceUseCase;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - PaymentWebhookController (Webhooks Pagar.me V5)")
class PaymentWebhookControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private EcommerceUseCase ecommerceUseCase;

    @InjectMocks
    private PaymentWebhookController webhookController;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        webhookController = new PaymentWebhookController(ecommerceUseCase, objectMapper);
        mockMvc = MockMvcBuilders.standaloneSetup(webhookController).build();
    }

    @Test
    @DisplayName("POST /api/v1/webhooks/pagarme - Deve receber evento e retornar 200 OK com confirmação de processamento")
    void shouldHandlePagarmeWebhook() throws Exception {
        String payload = """
            {
              "id": "hook_123456789",
              "type": "order.paid",
              "data": {
                "id": "or_abcdef123456",
                "status": "paid",
                "amount": 10000
              }
            }
        """;

        mockMvc.perform(post("/api/v1/webhooks/pagarme")
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.eventId").value("hook_123456789"))
                .andExpect(jsonPath("$.processed").value(true));

        verify(ecommerceUseCase).processPagarmeWebhook(eq(tenantId), eq("hook_123456789"), eq("order.paid"), anyString());
    }
}
