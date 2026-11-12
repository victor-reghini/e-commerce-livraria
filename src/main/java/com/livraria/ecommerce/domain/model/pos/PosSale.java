package com.livraria.ecommerce.domain.model.pos;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PosSale {

    private UUID id;
    private UUID tenantId;
    private UUID companyId;
    private UUID shiftId;
    private String saleNumber;
    private UUID clientUuid; // Chave de idempotência para vendas offline
    private String customerName;
    private String customerCpfCnpj;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private BigDecimal totalPaid;
    private BigDecimal changeAmount;
    private SaleStatus status;
    private String nfceKey;
    private String nfceXml;
    private NfceStatus nfceStatus;
    private List<PosSaleItem> items;
    private List<PosSalePayment> payments;
    private UUID createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    public PosSale(UUID id, UUID tenantId, UUID companyId, UUID shiftId, String saleNumber,
                   UUID clientUuid, String customerName, String customerCpfCnpj,
                   BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal totalPaid,
                   BigDecimal changeAmount, SaleStatus status, String nfceKey, String nfceXml,
                   NfceStatus nfceStatus, List<PosSaleItem> items, List<PosSalePayment> payments,
                   UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.shiftId = shiftId;
        this.saleNumber = saleNumber;
        this.clientUuid = clientUuid;
        this.customerName = customerName;
        this.customerCpfCnpj = customerCpfCnpj;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.payments = payments != null ? new ArrayList<>(payments) : new ArrayList<>();
        this.status = status != null ? status : SaleStatus.CONCLUIDA;
        this.nfceKey = nfceKey;
        this.nfceXml = nfceXml;
        this.nfceStatus = nfceStatus != null ? nfceStatus : NfceStatus.EMITIDA;
        this.createdBy = createdBy;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();

        recalculateTotals(discount);
        validatePayments();
    }

    private void recalculateTotals(BigDecimal explicitDiscount) {
        this.subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal itemsDiscount = items.stream()
                .map(PosSaleItem::getDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.discount = (explicitDiscount != null && explicitDiscount.compareTo(BigDecimal.ZERO) > 0)
                ? explicitDiscount
                : itemsDiscount;

        this.total = this.subtotal.subtract(this.discount).max(BigDecimal.ZERO);

        this.totalPaid = payments.stream()
                .map(PosSalePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (this.totalPaid.compareTo(this.total) > 0) {
            this.changeAmount = this.totalPaid.subtract(this.total);
        } else {
            this.changeAmount = BigDecimal.ZERO;
        }
    }

    private void validatePayments() {
        if (items.isEmpty()) {
            throw new BusinessException("A venda no PDV deve conter ao menos um item.");
        }
        if (totalPaid.compareTo(total) < 0) {
            throw new BusinessException("O valor total pago (R$ " + totalPaid + ") é menor que o total da venda (R$ " + total + ").");
        }
    }

    public void cancel() {
        if (this.status == SaleStatus.CANCELADA) {
            throw new BusinessException("A venda já está cancelada.");
        }
        this.status = SaleStatus.CANCELADA;
        this.nfceStatus = NfceStatus.CANCELADA;
        this.updatedAt = Instant.now();
    }

    public void setNfceData(String key, String xml, NfceStatus nfceStatus) {
        this.nfceKey = key;
        this.nfceXml = xml;
        this.nfceStatus = nfceStatus;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getCompanyId() { return companyId; }
    public UUID getShiftId() { return shiftId; }
    public String getSaleNumber() { return saleNumber; }
    public UUID getClientUuid() { return clientUuid; }
    public String getCustomerName() { return customerName; }
    public String getCustomerCpfCnpj() { return customerCpfCnpj; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public BigDecimal getChangeAmount() { return changeAmount; }
    public SaleStatus getStatus() { return status; }
    public String getNfceKey() { return nfceKey; }
    public String getNfceXml() { return nfceXml; }
    public NfceStatus getNfceStatus() { return nfceStatus; }
    public List<PosSaleItem> getItems() { return Collections.unmodifiableList(items); }
    public List<PosSalePayment> getPayments() { return Collections.unmodifiableList(payments); }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
