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

    default AuditLog recordEvent(String action, String resourceType, String resourceId, Object details) {
        return recordEvent(null, null, null, null, action, resourceType, resourceId, details);
    }

    default AuditLog recordEvent(String action, String resourceType, String resourceId, String userEmail, String userRole, Object details) {
        return recordEvent(null, userEmail, userRole, null, action, resourceType, resourceId, details);
    }

    List<AuditLog> queryLogs(String action, String resourceType, Instant from, Instant to, int page, int size);

    long countLogs(String action, String resourceType, Instant from, Instant to);
}

