package com.livraria.ecommerce.infrastructure.adapters.in.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - ObservabilityController (Métricas e Telemetria)")
class ObservabilityControllerTest {

    private MockMvc mockMvc;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MeterRegistry meterRegistry;

    private ObservabilityController controller;

    @BeforeEach
    void setUp() {
        controller = new ObservabilityController(meterRegistry);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /api/v1/observability/summary - Deve retornar resumo consolidado de métricas e links")
    void shouldReturnMetricsSummary() throws Exception {
        Counter counter = org.mockito.Mockito.mock(Counter.class);
        when(counter.count()).thenReturn(42.0);
        when(meterRegistry.find(anyString()).counter()).thenReturn(counter);

        mockMvc.perform(get("/api/v1/observability/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders_created").value(42.0))
                .andExpect(jsonPath("$.telemetry_endpoints.prometheus_metrics").value("/actuator/prometheus"))
                .andExpect(jsonPath("$.telemetry_endpoints.health_check").value("/actuator/health"));
    }
}

