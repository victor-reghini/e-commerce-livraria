package com.livraria.ecommerce.domain.model.stock;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StockTransfer {

    private UUID id;
    private UUID tenantId;
    private String transferNumber;
    private UUID originCompanyId;
    private String originCompanyName;
    private UUID destinationCompanyId;
    private String destinationCompanyName;
    private TransferStatus status;
    private TransferType transferType;
    private String notes;
    private List<StockTransferItem> items;
    private Instant dispatchedAt;
    private Instant receivedAt;
    private UUID createdBy;
    private UUID receivedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public StockTransfer(UUID id, UUID tenantId, String transferNumber, UUID originCompanyId,
                         String originCompanyName, UUID destinationCompanyId, String destinationCompanyName,
                         TransferStatus status, TransferType transferType, String notes,
                         List<StockTransferItem> items, Instant dispatchedAt, Instant receivedAt,
                         UUID createdBy, UUID receivedBy, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.transferNumber = transferNumber;
        this.originCompanyId = originCompanyId;
        this.originCompanyName = originCompanyName;
        this.destinationCompanyId = destinationCompanyId;
        this.destinationCompanyName = destinationCompanyName;
        this.status = status != null ? status : TransferStatus.CRIADO;
        this.transferType = transferType != null ? transferType : TransferType.REMESSA_EVENTO;
        this.notes = notes;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.dispatchedAt = dispatchedAt;
        this.receivedAt = receivedAt;
        this.createdBy = createdBy;
        this.receivedBy = receivedBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();

        validate();
    }

    private void validate() {
        if (originCompanyId.equals(destinationCompanyId)) {
            throw new BusinessException("A filial de origem e a filial de destino não podem ser a mesma.");
        }
    }

    public void dispatch(UUID dispatchedBy) {
        if (status != TransferStatus.CRIADO && status != TransferStatus.RESERVADO) {
            throw new BusinessException("A transferência não pode ser despachada no status atual: " + status);
        }
        if (items.isEmpty()) {
            throw new BusinessException("Não é possível despachar uma transferência sem itens.");
        }
        for (StockTransferItem item : items) {
            item.setQuantityDispatched(item.getQuantityRequested());
        }
        this.status = TransferStatus.EM_TRANSITO;
        this.dispatchedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void receive(Map<UUID, Integer> receivedQuantities, UUID receivedBy) {
        if (status != TransferStatus.EM_TRANSITO) {
            throw new BusinessException("Apenas transferências EM_TRANSITO podem ser recebidas.");
        }

        boolean hasDivergence = false;
        for (StockTransferItem item : items) {
            int received = receivedQuantities != null
                    ? receivedQuantities.getOrDefault(item.getSkuId(), item.getQuantityDispatched())
                    : item.getQuantityDispatched();
            item.setQuantityReceived(received);
            if (received != item.getQuantityDispatched()) {
                hasDivergence = true;
            }
        }

        this.status = hasDivergence ? TransferStatus.DIVERGENCIA : TransferStatus.RECEBIDO;
        this.receivedBy = receivedBy;
        this.receivedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (status == TransferStatus.RECEBIDO || status == TransferStatus.EM_TRANSITO) {
            throw new BusinessException("Não é possível cancelar uma transferência despachada ou recebida.");
        }
        this.status = TransferStatus.CANCELADO;
        this.updatedAt = Instant.now();
    }

    public int calculateTotalItems() {
        return items.stream().mapToInt(StockTransferItem::getQuantityRequested).sum();
    }

    public BigDecimal calculateTotalValue() {
        return items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantityRequested())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getTransferNumber() { return transferNumber; }
    public UUID getOriginCompanyId() { return originCompanyId; }
    public String getOriginCompanyName() { return originCompanyName; }
    public UUID getDestinationCompanyId() { return destinationCompanyId; }
    public String getDestinationCompanyName() { return destinationCompanyName; }
    public TransferStatus getStatus() { return status; }
    public TransferType getTransferType() { return transferType; }
    public String getNotes() { return notes; }
    public List<StockTransferItem> getItems() { return Collections.unmodifiableList(items); }
    public Instant getDispatchedAt() { return dispatchedAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getReceivedBy() { return receivedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
