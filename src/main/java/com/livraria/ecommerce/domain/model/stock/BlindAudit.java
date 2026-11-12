package com.livraria.ecommerce.domain.model.stock;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BlindAudit {

    private UUID id;
    private UUID tenantId;
    private UUID companyId;
    private String companyName;
    private UUID transferId;
    private String code;
    private AuditStatus status;
    private UUID auditorId;
    private String notes;
    private List<BlindAuditItem> items;
    private Instant createdAt;
    private Instant completedAt;

    public BlindAudit(UUID id, UUID tenantId, UUID companyId, String companyName, UUID transferId,
                      String code, AuditStatus status, UUID auditorId, String notes,
                      List<BlindAuditItem> items, Instant createdAt, Instant completedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.companyName = companyName;
        this.transferId = transferId;
        this.code = code;
        this.status = status != null ? status : AuditStatus.ABERTA;
        this.auditorId = auditorId;
        this.notes = notes;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.completedAt = completedAt;
    }

    public void scanBarcode(UUID skuId, String skuCode, String title, int count) {
        if (status == AuditStatus.FINALIZADA || status == AuditStatus.CONCILIADA) {
            throw new BusinessException("Não é possível bipar itens em uma conferência já finalizada.");
        }
        this.status = AuditStatus.EM_BIPAGEM;

        Optional<BlindAuditItem> existing = items.stream()
                .filter(i -> i.getSkuId().equals(skuId))
                .findFirst();

        if (existing.isPresent()) {
            existing.get().incrementScanned(count);
        } else {
            BlindAuditItem newItem = new BlindAuditItem(
                    UUID.randomUUID(), tenantId, id, skuId, skuCode, title, count, 0, count, Instant.now()
            );
            this.items.add(newItem);
        }
    }

    public void finalizeAudit() {
        if (status == AuditStatus.FINALIZADA || status == AuditStatus.CONCILIADA) {
            throw new BusinessException("A conferência já se encontra finalizada.");
        }
        this.status = AuditStatus.FINALIZADA;
        this.completedAt = Instant.now();
    }

    public void reconcile() {
        if (status != AuditStatus.FINALIZADA) {
            throw new BusinessException("Apenas conferências FINALIZADAS podem ser conciliadas no estoque.");
        }
        this.status = AuditStatus.CONCILIADA;
    }

    public int getTotalScanned() {
        return items.stream().mapToInt(BlindAuditItem::getQuantityScanned).sum();
    }

    public int getTotalExpected() {
        return items.stream().mapToInt(BlindAuditItem::getQuantityExpected).sum();
    }

    public int getTotalDivergence() {
        return getTotalScanned() - getTotalExpected();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCompanyId() { return companyId; }
    public String getCompanyName() { return companyName; }
    public UUID getTransferId() { return transferId; }
    public String getCode() { return code; }
    public AuditStatus getStatus() { return status; }
    public UUID getAuditorId() { return auditorId; }
    public String getNotes() { return notes; }
    public List<BlindAuditItem> getItems() { return Collections.unmodifiableList(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
