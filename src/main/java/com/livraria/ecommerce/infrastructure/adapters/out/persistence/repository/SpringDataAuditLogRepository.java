package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    @Query("SELECT a FROM AuditLogJpaEntity a WHERE a.tenantId = :tenantId " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:resourceType IS NULL OR a.resourceType = :resourceType) " +
            "AND (:from IS NULL OR a.createdAt >= :from) " +
            "AND (:to IS NULL OR a.createdAt <= :to) " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLogJpaEntity> findLogsFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("SELECT COUNT(a) FROM AuditLogJpaEntity a WHERE a.tenantId = :tenantId " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:resourceType IS NULL OR a.resourceType = :resourceType) " +
            "AND (:from IS NULL OR a.createdAt >= :from) " +
            "AND (:to IS NULL OR a.createdAt <= :to)")
    long countLogsFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("action") String action,
            @Param("resourceType") String resourceType,
            @Param("from") Instant from,
            @Param("to") Instant to
    );
}
