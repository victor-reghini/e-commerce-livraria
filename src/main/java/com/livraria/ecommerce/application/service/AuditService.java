package com.livraria.ecommerce.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.domain.model.audit.AuditLog;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import com.livraria.ecommerce.domain.port.out.AuditRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuditService implements AuditUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private static final Pattern CARD_TOKEN_PATTERN = Pattern.compile("(tok_[a-zA-Z0-9_]{4})[a-zA-Z0-9_]+([a-zA-Z0-9_]{4})");
    private static final Pattern PAN_PATTERN = Pattern.compile("(\\b\\d{4})\\d{8,11}(\\d{4}\\b)");
    private static final Pattern CPF_PATTERN = Pattern.compile("(\\b\\d{3})\\.?(\\d{3})\\.?(\\d{3})-?(\\d{2}\\b)");

    private final AuditRepositoryPort auditRepository;
    private final ObservabilityMetricsService metricsService;
    private final ObjectMapper objectMapper;

    public AuditService(AuditRepositoryPort auditRepository,
                        ObservabilityMetricsService metricsService,
                        ObjectMapper objectMapper) {
        this.auditRepository = auditRepository;
        this.metricsService = metricsService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AuditLog recordEvent(
            UUID userId,
            String userEmail,
            String userRole,
            String clientIp,
            String action,
            String resourceType,
            String resourceId,
            Object details) {

        UUID tenantId = TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        String detailsJson = null;
        if (details != null) {
            try {
                String rawJson = (details instanceof String) ? (String) details : objectMapper.writeValueAsString(details);
                detailsJson = maskSensitiveData(rawJson);
            } catch (Exception e) {
                log.warn("Erro ao serializar payload de auditoria para JSON: {}", e.getMessage());
                detailsJson = "{\"error\": \"Falha de serialização\", \"raw\": \"" + maskSensitiveData(details.toString()) + "\"}";
            }
        }

        AuditLog auditLog = AuditLog.of(
                tenantId,
                userId,
                userEmail,
                userRole,
                clientIp != null ? clientIp : "127.0.0.1",
                action != null ? action.toUpperCase() : "GENERAL_ACTION",
                resourceType != null ? resourceType.toUpperCase() : "RESOURCE",
                resourceId,
                detailsJson
        );

        AuditLog saved = auditRepository.save(auditLog);
        metricsService.recordAuditEvent(action);
        log.info("Evento de auditoria imutável registrado: action={}, resourceType={}, resourceId={}, user={}",
                action, resourceType, resourceId, userEmail);

        return saved;
    }

    @Override
    public List<AuditLog> queryLogs(String action, String resourceType, Instant from, Instant to, int page, int size) {
        UUID tenantId = TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return auditRepository.findLogs(tenantId, action, resourceType, from, to, page, size);
    }

    @Override
    public long countLogs(String action, String resourceType, Instant from, Instant to) {
        UUID tenantId = TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return auditRepository.countLogs(tenantId, action, resourceType, from, to);
    }

    /**
     * Aplica mascaramento de dados sensíveis conforme padrões de conformidade LGPD e PCI-DSS.
     */
    public String maskSensitiveData(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        // 1. Mascaramento PCI-DSS: Cartão de Crédito PAN 4111111111111111 -> 4111********1111
        String masked = PAN_PATTERN.matcher(input).replaceAll("$1********$2");

        // 2. Mascaramento de Gateway Tokens: tok_1234567890 -> tok_1234****7890
        masked = CARD_TOKEN_PATTERN.matcher(masked).replaceAll("$1********$2");

        // 3. Mascaramento LGPD: CPF 123.456.789-00 -> ***.456.789-**
        masked = CPF_PATTERN.matcher(masked).replaceAll("***.$2.$3-**");

        // 4. Mascaramento de senhas e secrets em JSON
        masked = masked.replaceAll("(?i)\"(password|secret|cvv|securityCode|pin)\"\\s*:\\s*\"[^\"]+\"", "\"$1\":\"***REDACTED***\"");

        return masked;
    }
}
