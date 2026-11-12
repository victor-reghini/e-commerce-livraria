package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.domain.port.in.EcommerceUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Recepção assíncrona de webhooks de pagamento (Pagar.me V5) com idempotência estrita")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final EcommerceUseCase ecommerceUseCase;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(EcommerceUseCase ecommerceUseCase, ObjectMapper objectMapper) {
        this.ecommerceUseCase = ecommerceUseCase;
        this.objectMapper = objectMapper;
    }

    private UUID getTenantId(String headerTenantId) {
        if (headerTenantId != null && !headerTenantId.isBlank()) {
            return UUID.fromString(headerTenantId);
        }
        return TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    @PostMapping("/pagarme")
    @Operation(summary = "Endpoint assíncrono para eventos do Pagar.me V5 com garantia de idempotência")
    public ResponseEntity<Map<String, Object>> handlePagarmeWebhook(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantIdHeader,
            @RequestHeader(value = "X-Hub-Signature", required = false) String signature,
            @RequestBody String rawPayload) {
        UUID tenantId = getTenantId(tenantIdHeader);

        try {
            JsonNode json = objectMapper.readTree(rawPayload);
            String eventId = json.has("id") ? json.get("id").asText() : "evt_" + UUID.randomUUID().toString().replace("-", "");
            String eventType = json.has("type") ? json.get("type").asText() : "order.paid";

            log.info("Processando webhook Pagar.me V5: eventId={}, eventType={}", eventId, eventType);
            ecommerceUseCase.processPagarmeWebhook(tenantId, eventId, eventType, rawPayload);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "eventId", eventId,
                    "processed", true
            ));
        } catch (Exception e) {
            log.error("Erro ao processar webhook Pagar.me: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }
}
