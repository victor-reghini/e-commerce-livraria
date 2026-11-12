package com.livraria.ecommerce.application.dto.audit;

import com.livraria.ecommerce.domain.model.audit.AuditLog;

import java.time.Instant;
import java.util.UUID;

public class AuditDto {

    public record AuditLogResponse(
            UUID id,
            UUID tenantId,
            UUID userId,
            String userEmail,
            String userRole,
            String clientIp,
            String action,
            String resourceType,
            String resourceId,
            String detailsJson,
            Instant createdAt
    ) {
        public static AuditLogResponse fromDomain(AuditLog log) {
            return new AuditLogResponse(
                    log.getId(),
                    log.getTenantId(),
                    log.getUserId(),
                    log.getUserEmail(),
                    log.getUserRole(),
                    log.getClientIp(),
                    log.getAction(),
                    log.getResourceType(),
                    log.getResourceId(),
                    log.getDetailsJson(),
                    log.getCreatedAt()
            );
        }
    }

    public record AuditLogPageResponse(
            java.util.List<AuditLogResponse> logs,
            long totalElements,
            int page,
            int size
    ) {}
}
