package com.livraria.ecommerce.infrastructure.adapters.in.web.exception;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        Map<String, List<String>> validationErrors
) {
    public static ApiErrorResponse of(int status, String error, String message, String path, String correlationId) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, correlationId, null);
    }

    public static ApiErrorResponse ofValidation(int status, String error, String message, String path,
                                                String correlationId, Map<String, List<String>> validationErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path, correlationId, validationErrors);
    }
}

