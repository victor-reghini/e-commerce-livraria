package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.domain.model.audit.AuditLog;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - AuditController (Consulta de Trilha de Auditoria LGPD)")
class AuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditUseCase auditUseCase;

    private AuditController auditController;

    @BeforeEach
    void setUp() {
        auditController = new AuditController(auditUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(auditController).build();
    }

    @Test
    @DisplayName("GET /api/v1/audit/logs - Deve retornar página de logs de auditoria")
    void shouldQueryAuditLogs() throws Exception {
        UUID id = UUID.randomUUID();
        AuditLog log = AuditLog.of(
                UUID.randomUUID(), UUID.randomUUID(), "admin@livraria.com", "ADMIN",
                "127.0.0.1", "USER_LOGIN", "USER", "user-123", "{\"status\":\"SUCCESS\"}"
        );
        log.setId(id);

        when(auditUseCase.queryLogs(eq("USER_LOGIN"), eq("USER"), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(log));
        when(auditUseCase.countLogs(eq("USER_LOGIN"), eq("USER"), any(), any()))
                .thenReturn(1L);

        mockMvc.perform(get("/api/v1/audit/logs")
                        .param("action", "USER_LOGIN")
                        .param("resourceType", "USER")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.logs[0].id").value(id.toString()))
                .andExpect(jsonPath("$.logs[0].action").value("USER_LOGIN"))
                .andExpect(jsonPath("$.logs[0].userEmail").value("admin@livraria.com"))
                .andExpect(jsonPath("$.logs[0].clientIp").value("127.0.0.1"));

        verify(auditUseCase).queryLogs(eq("USER_LOGIN"), eq("USER"), any(), any(), eq(0), eq(20));
    }
}

