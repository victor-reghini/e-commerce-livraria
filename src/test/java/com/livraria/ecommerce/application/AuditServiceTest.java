package com.livraria.ecommerce.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.application.service.AuditService;
import com.livraria.ecommerce.application.service.ObservabilityMetricsService;
import com.livraria.ecommerce.domain.model.audit.AuditLog;
import com.livraria.ecommerce.domain.port.out.AuditRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - AuditService (Auditoria Imutável e Mascaramento LGPD/PCI-DSS)")
class AuditServiceTest {

    @Mock
    private AuditRepositoryPort auditRepository;

    @Mock
    private ObservabilityMetricsService metricsService;

    private ObjectMapper objectMapper;
    private AuditService auditService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditService = new AuditService(auditRepository, metricsService, objectMapper);
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        lenient().when(auditRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deve registrar log de auditoria com CPF mascarado (LGPD)")
    void shouldMaskCpfForLgpdCompliance() {
        UUID userId = UUID.randomUUID();
        Map<String, Object> details = Map.of(
                "customerName", "João Silva",
                "customerCpf", "123.456.789-01",
                "operation", "CUSTOMER_DATA_ACCESSED"
        );

        AuditLog log = auditService.recordEvent(
                userId,
                "admin@livraria.com",
                "ADMIN",
                "192.168.1.100",
                "CUSTOMER_VIEW",
                "CUSTOMER",
                "123",
                details
        );

        assertNotNull(log);
        assertEquals("CUSTOMER_VIEW", log.getAction());
        assertEquals("CUSTOMER", log.getResourceType());
        assertEquals("admin@livraria.com", log.getUserEmail());

        // Validação de mascaramento LGPD: CPF 123.456.789-01 -> ***.456.789-**
        assertNotNull(log.getDetailsJson());
        assertFalse(log.getDetailsJson().contains("123.456.789-01"), "CPF puro não pode constar no log");
        assertTrue(log.getDetailsJson().contains("***.456.789-**"), "CPF deve estar mascarado");

        verify(metricsService).recordAuditEvent("CUSTOMER_VIEW");
    }

    @Test
    @DisplayName("Deve mascarar tokens e dados de cartão de crédito (PCI-DSS)")
    void shouldMaskCreditCardDataForPciCompliance() {
        String inputWithCard = "{\"token\":\"tok_1a2b3c4d5e6f7g8h9i0j\",\"cardPan\":\"4111222233334444\",\"password\":\"secret123\"}";
        String masked = auditService.maskSensitiveData(inputWithCard);

        assertNotNull(masked);
        assertFalse(masked.contains("4111222233334444"));
        assertFalse(masked.contains("secret123"));
        assertTrue(masked.contains("4111********4444"));
        assertTrue(masked.contains("***REDACTED***"));
    }

    @Test
    @DisplayName("Deve consultar logs de auditoria filtrados")
    void shouldQueryLogs() {
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        List<AuditLog> expectedLogs = List.of(new AuditLog());

        when(auditRepository.findLogs(eq(tenantId), eq("USER_LOGIN"), eq("USER"), eq(from), eq(to), eq(0), eq(20)))
                .thenReturn(expectedLogs);
        when(auditRepository.countLogs(eq(tenantId), eq("USER_LOGIN"), eq("USER"), eq(from), eq(to)))
                .thenReturn(1L);

        List<AuditLog> logs = auditService.queryLogs("USER_LOGIN", "USER", from, to, 0, 20);
        long count = auditService.countLogs("USER_LOGIN", "USER", from, to);

        assertEquals(1, logs.size());
        assertEquals(1L, count);
    }
}
