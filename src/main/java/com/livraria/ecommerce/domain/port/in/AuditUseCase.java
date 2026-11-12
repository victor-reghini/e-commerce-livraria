package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.domain.model.audit.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditUseCase {

    AuditLog recordEvent(
            UUID userId,
            String userEmail,
            String userRole,
            String clientIp,
            String action,
            String resourceType,
            String resourceId,
            Object details
    );

    List<AuditLog> queryLogs(String action, String resourceType, Instant from, Instant to, int page, int size);

    long countLogs(String action, String resourceType, Instant from, Instant to);
}

