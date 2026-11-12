package com.livraria.ecommerce.domain.model.fiscal;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FiscalInvoice {

    private UUID id;
    private UUID tenantId;
    private UUID companyId;
    private UUID orderId;
    private UUID saleId;
    private FiscalModel model;
    private int series;
    private long invoiceNumber;
    private String accessKey;
    private FiscalStatus status;
    private String natureOfOperation;
    private BigDecimal totalAmount;
    private BigDecimal totalDiscount;
    private BigDecimal totalTaxIcms;
    private BigDecimal totalTaxPis;
    private BigDecimal totalTaxCofins;
    private String protocolNumber;
    private String xmlPayload;
    private String danfeUrl;
    private String rejectionReason;
    private String cancellationReason;
    private Instant createdAt;
    private Instant authorizedAt;
    private Instant canceledAt;

    public FiscalInvoice() {
        this.id = UUID.randomUUID();
        this.status = FiscalStatus.EMITIDA;
        this.natureOfOperation = "VENDA DE MERCADORIAS";
        this.series = 1;
        this.totalAmount = BigDecimal.ZERO;
        this.totalDiscount = BigDecimal.ZERO;
        this.totalTaxIcms = BigDecimal.ZERO;
        this.totalTaxPis = BigDecimal.ZERO;
        this.totalTaxCofins = BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    public static FiscalInvoice createForOrder(
            UUID tenantId,
            UUID companyId,
            UUID orderId,
            int series,
            long invoiceNumber,
            String accessKey,
            BigDecimal totalAmount,
            BigDecimal totalDiscount,
            String natureOfOperation) {
        FiscalInvoice invoice = new FiscalInvoice();
        invoice.tenantId = tenantId;
        invoice.companyId = companyId;
        invoice.orderId = orderId;
        invoice.model = FiscalModel.NFE_55;
        invoice.series = series > 0 ? series : 1;
        invoice.invoiceNumber = invoiceNumber;
        invoice.accessKey = accessKey;
        invoice.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        invoice.totalDiscount = totalDiscount != null ? totalDiscount : BigDecimal.ZERO;
        if (natureOfOperation != null && !natureOfOperation.isBlank()) {
            invoice.natureOfOperation = natureOfOperation;
        }
        return invoice;
    }

    public static FiscalInvoice createForPosSale(
            UUID tenantId,
            UUID companyId,
            UUID saleId,
            int series,
            long invoiceNumber,
            String accessKey,
            BigDecimal totalAmount,
            BigDecimal totalDiscount) {
        FiscalInvoice invoice = new FiscalInvoice();
        invoice.tenantId = tenantId;
        invoice.companyId = companyId;
        invoice.saleId = saleId;
        invoice.model = FiscalModel.NFCE_65;
        invoice.series = series > 0 ? series : 1;
        invoice.invoiceNumber = invoiceNumber;
        invoice.accessKey = accessKey;
        invoice.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        invoice.totalDiscount = totalDiscount != null ? totalDiscount : BigDecimal.ZERO;
        invoice.natureOfOperation = "VENDA A CONSUMIDOR FINAL PDV";
        return invoice;
    }

    public void authorize(String protocol, String xml) {
        if (this.status == FiscalStatus.CANCELADA) {
            throw new BusinessException("Não é possível autorizar uma nota fiscal que já foi cancelada.");
        }
        this.status = FiscalStatus.AUTORIZADA;
        this.protocolNumber = protocol;
        this.xmlPayload = xml;
        this.authorizedAt = Instant.now();
        this.rejectionReason = null;
    }

    public void markAsContingency(String xml, String reason) {
        this.status = FiscalStatus.CONTINGENCIA;
        this.xmlPayload = xml;
        this.rejectionReason = reason;
    }

    public void reject(String reason) {
        this.status = FiscalStatus.REJEITADA;
        this.rejectionReason = reason;
    }

    public void cancel(String protocol, String reason) {
        if (this.status != FiscalStatus.AUTORIZADA && this.status != FiscalStatus.CONTINGENCIA) {
            throw new BusinessException("Somente notas fiscais AUTORIZADAS ou em CONTINGÊNCIA podem ser canceladas. Status atual: " + this.status);
        }
        if (reason == null || reason.trim().length() < 15) {
            throw new BusinessException("A justificativa de cancelamento perante a SEFAZ deve conter no mínimo 15 caracteres.");
        }
        this.status = FiscalStatus.CANCELADA;
        this.protocolNumber = protocol;
        this.cancellationReason = reason;
        this.canceledAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getSaleId() { return saleId; }
    public void setSaleId(UUID saleId) { this.saleId = saleId; }
    public FiscalModel getModel() { return model; }
    public void setModel(FiscalModel model) { this.model = model; }
    public int getSeries() { return series; }
    public void setSeries(int series) { this.series = series; }
    public long getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(long invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public FiscalStatus getStatus() { return status; }
    public void setStatus(FiscalStatus status) { this.status = status; }
    public String getNatureOfOperation() { return natureOfOperation; }
    public void setNatureOfOperation(String natureOfOperation) { this.natureOfOperation = natureOfOperation; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getTotalDiscount() { return totalDiscount; }
    public void setTotalDiscount(BigDecimal totalDiscount) { this.totalDiscount = totalDiscount; }
    public BigDecimal getTotalTaxIcms() { return totalTaxIcms; }
    public void setTotalTaxIcms(BigDecimal totalTaxIcms) { this.totalTaxIcms = totalTaxIcms; }
    public BigDecimal getTotalTaxPis() { return totalTaxPis; }
    public void setTotalTaxPis(BigDecimal totalTaxPis) { this.totalTaxPis = totalTaxPis; }
    public BigDecimal getTotalTaxCofins() { return totalTaxCofins; }
    public void setTotalTaxCofins(BigDecimal totalTaxCofins) { this.totalTaxCofins = totalTaxCofins; }
    public String getProtocolNumber() { return protocolNumber; }
    public void setProtocolNumber(String protocolNumber) { this.protocolNumber = protocolNumber; }
    public String getXmlPayload() { return xmlPayload; }
    public void setXmlPayload(String xmlPayload) { this.xmlPayload = xmlPayload; }
    public String getDanfeUrl() { return danfeUrl; }
    public void setDanfeUrl(String danfeUrl) { this.danfeUrl = danfeUrl; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(Instant authorizedAt) { this.authorizedAt = authorizedAt; }
    public Instant getCanceledAt() { return canceledAt; }
    public void setCanceledAt(Instant canceledAt) { this.canceledAt = canceledAt; }
}
