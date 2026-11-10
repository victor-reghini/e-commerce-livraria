package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Monitoramento básico de saúde e status da infraestrutura")
public class HealthCheckController {

    @Value("${app.environment:development}")
    private String environment;

    @Value("${spring.application.name:ecommerce-backend}")
    private String applicationName;

    @GetMapping
    @Operation(summary = "Verifica integridade da aplicação e suporte a Virtual Threads")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        boolean isVirtual = Thread.currentThread().isVirtual();
        String currentTenant = TenantContext.getTenantIdAsString();

        Map<String, Object> response = Map.of(
                "status", "UP",
                "application", applicationName,
                "environment", environment,
                "virtualThreadsActive", isVirtual,
                "currentTenantId", currentTenant != null ? currentTenant : "none",
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.ok(response);
    }
}

