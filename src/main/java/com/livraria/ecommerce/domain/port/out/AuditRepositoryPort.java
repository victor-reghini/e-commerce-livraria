package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.audit.AuditLog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditRepositoryPort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findLogs(UUID tenantId, String action, String resourceType, Instant from, Instant to, int page, int size);

    long countLogs(UUID tenantId, String action, String resourceType, Instant from, Instant to);
}
