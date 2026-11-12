package com.livraria.ecommerce.domain.model.audit;

import java.time.Instant;
import java.util.UUID;

public class AuditLog {

    private UUID id;
    private UUID tenantId;
    private UUID userId;
    private String userEmail;
    private String userRole;
    private String clientIp;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detailsJson;
    private Instant createdAt;

    public AuditLog() {
        this.createdAt = Instant.now();
    }

    public static AuditLog of(
            UUID tenantId,
            UUID userId,
            String userEmail,
            String userRole,
            String clientIp,
            String action,
            String resourceType,
            String resourceId,
            String detailsJson) {
        AuditLog log = new AuditLog();
        log.tenantId = tenantId;
        log.userId = userId;
        log.userEmail = userEmail;
        log.userRole = userRole;
        log.clientIp = clientIp;
        log.action = action;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.detailsJson = detailsJson;
        log.createdAt = Instant.now();
        return log;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public String getDetailsJson() { return detailsJson; }
    public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

