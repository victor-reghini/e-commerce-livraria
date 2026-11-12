package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.audit.AuditLog;
import com.livraria.ecommerce.domain.port.out.AuditRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.AuditLogJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataAuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class AuditPersistenceAdapter implements AuditRepositoryPort {

    private final SpringDataAuditLogRepository auditRepository;

    public AuditPersistenceAdapter(SpringDataAuditLogRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(UUID.randomUUID());
        }
        AuditLogJpaEntity entity = toEntity(auditLog);
        AuditLogJpaEntity saved = auditRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> findLogs(UUID tenantId, String action, String resourceType, Instant from, Instant to, int page, int size) {
        return auditRepository.findLogsFiltered(tenantId, action, resourceType, from, to, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countLogs(UUID tenantId, String action, String resourceType, Instant from, Instant to) {
        return auditRepository.countLogsFiltered(tenantId, action, resourceType, from, to);
    }

    private AuditLogJpaEntity toEntity(AuditLog log) {
        return AuditLogJpaEntity.builder()
                .id(log.getId())
                .tenantId(log.getTenantId())
                .userId(log.getUserId())
                .userEmail(log.getUserEmail())
                .userRole(log.getUserRole())
                .clientIp(log.getClientIp())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .detailsJson(log.getDetailsJson())
                .createdAt(log.getCreatedAt() != null ? log.getCreatedAt() : Instant.now())
                .build();
    }

    private AuditLog toDomain(AuditLogJpaEntity entity) {
        AuditLog log = new AuditLog();
        log.setId(entity.getId());
        log.setTenantId(entity.getTenantId());
        log.setUserId(entity.getUserId());
        log.setUserEmail(entity.getUserEmail());
        log.setUserRole(entity.getUserRole());
        log.setClientIp(entity.getClientIp());
        log.setAction(entity.getAction());
        log.setResourceType(entity.getResourceType());
        log.setResourceId(entity.getResourceId());
        log.setDetailsJson(entity.getDetailsJson());
        log.setCreatedAt(entity.getCreatedAt());
        return log;
    }
}
