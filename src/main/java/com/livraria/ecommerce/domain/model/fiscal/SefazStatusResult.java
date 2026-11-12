package com.livraria.ecommerce.domain.model.fiscal;

import java.time.Instant;

public record SefazStatusResult(
        boolean operational,
        int statusCode,
        String statusMessage,
        String uf,
        String environment,
        long averageResponseTimeMs,
        Instant checkedAt
) {
    public static SefazStatusResult operational(String uf, String environment, long responseTimeMs) {
        return new SefazStatusResult(true, 107, "Servico em Operacao", uf, environment, responseTimeMs, Instant.now());
    }

    public static SefazStatusResult degraded(String uf, String environment, String message, long responseTimeMs) {
        return new SefazStatusResult(false, 108, "Serviço Paralisado Momentaneamente: " + message, uf, environment, responseTimeMs, Instant.now());
    }
}
