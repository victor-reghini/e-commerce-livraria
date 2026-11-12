package com.livraria.ecommerce.domain.model.fiscal;

import java.time.Instant;

public record FiscalCancellationResult(
        boolean success,
        int statusCode,
        String statusMessage,
        String protocolNumber,
        String accessKey,
        Instant canceledAt
) {
    public static FiscalCancellationResult success(String protocolNumber, String accessKey) {
        return new FiscalCancellationResult(true, 135, "Evento registrado e vinculado a NF-e", protocolNumber, accessKey, Instant.now());
    }

    public static FiscalCancellationResult failure(int statusCode, String reason, String accessKey) {
        return new FiscalCancellationResult(false, statusCode, reason, null, accessKey, null);
    }
}
