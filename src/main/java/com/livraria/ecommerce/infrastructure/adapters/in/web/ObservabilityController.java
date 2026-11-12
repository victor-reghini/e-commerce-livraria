package com.livraria.ecommerce.infrastructure.adapters.in.web;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/observability")
@Tag(name = "Observabilidade e Métricas", description = "Dashboard e telemetria de métricas da aplicação")
public class ObservabilityController {

    private final MeterRegistry meterRegistry;

    public ObservabilityController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumo Consolidado de Métricas de Negócio e Telemetria")
    public ResponseEntity<Map<String, Object>> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();

        summary.put("orders_created", getCounterValue("ecommerce.orders.created"));
        summary.put("orders_paid", getCounterValue("ecommerce.orders.paid"));
        summary.put("pos_sales_completed", getCounterValue("pos.sales.completed"));
        summary.put("fiscal_invoices_canceled", getCounterValue("fiscal.invoices.canceled"));

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("prometheus_metrics", "/actuator/prometheus");
        endpoints.put("health_check", "/actuator/health");
        endpoints.put("api_health", "/api/v1/health");
        summary.put("telemetry_endpoints", endpoints);

        return ResponseEntity.ok(summary);
    }

    private double getCounterValue(String name) {
        try {
            var counter = meterRegistry.find(name).counter();
            return counter != null ? counter.count() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}

