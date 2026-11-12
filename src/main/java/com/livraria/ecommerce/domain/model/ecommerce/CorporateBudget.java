package com.livraria.ecommerce.domain.model.ecommerce;

import com.livraria.ecommerce.domain.exception.BusinessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CorporateBudget {

    private UUID id;
    private UUID tenantId;
    private String budgetNumber;
    private UUID customerId;
    private String corporateName;
    private String tradeName;
    private String cnpj;
    private String stateRegistration;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private BudgetStatus status;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingAmount;
    private BigDecimal totalAmount;
    private int paymentTermsDays;
    private LocalDate dueDate;
    private String notes;
    private String rejectionReason;
    private UUID invoicedOrderId;
    private List<CorporateBudgetItem> items;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant approvedAt;

    public CorporateBudget(UUID id, UUID tenantId, String budgetNumber, UUID customerId,
                           String corporateName, String tradeName, String cnpj, String stateRegistration,
                           String contactName, String contactEmail, String contactPhone,
                           BudgetStatus status, BigDecimal subtotal, BigDecimal discount,
                           BigDecimal shippingAmount, BigDecimal totalAmount, int paymentTermsDays,
                           LocalDate dueDate, String notes, String rejectionReason, UUID invoicedOrderId,
                           List<CorporateBudgetItem> items, Instant createdAt, Instant updatedAt,
                           Instant approvedAt) {
        if (tenantId == null) {
            throw new BusinessException("Tenant ID é obrigatório para o orçamento corporativo.");
        }
        if (budgetNumber == null || budgetNumber.isBlank()) {
            throw new BusinessException("Número do orçamento é obrigatório.");
        }
        if (corporateName == null || corporateName.isBlank()) {
            throw new BusinessException("Razão Social é obrigatória.");
        }
        if (cnpj == null || cnpj.isBlank()) {
            throw new BusinessException("CNPJ é obrigatório.");
        }
        if (contactEmail == null || contactEmail.isBlank()) {
            throw new BusinessException("E-mail de contato é obrigatório.");
        }
        this.id = id != null ? id : UUID.randomUUID();
        this.tenantId = tenantId;
        this.budgetNumber = budgetNumber;
        this.customerId = customerId;
        this.corporateName = corporateName;
        this.tradeName = tradeName;
        this.cnpj = cnpj.replaceAll("\\D", "");
        this.stateRegistration = stateRegistration;
        this.contactName = contactName != null ? contactName : corporateName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.status = status != null ? status : BudgetStatus.RASCUNHO;
        this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO;
        this.discount = discount != null ? discount : BigDecimal.ZERO;
        this.shippingAmount = shippingAmount != null ? shippingAmount : BigDecimal.ZERO;
        this.totalAmount = totalAmount != null ? totalAmount : subtotal.subtract(this.discount).add(this.shippingAmount);
        this.paymentTermsDays = paymentTermsDays > 0 ? paymentTermsDays : 30;
        this.dueDate = dueDate;
        this.notes = notes;
        this.rejectionReason = rejectionReason;
        this.invoicedOrderId = invoicedOrderId;
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.approvedAt = approvedAt;
    }

    public static CorporateBudget create(UUID tenantId, String budgetNumber, UUID customerId,
                                         String corporateName, String tradeName, String cnpj,
                                         String stateRegistration, String contactName, String contactEmail,
                                         String contactPhone, int paymentTermsDays, BigDecimal discount,
                                         BigDecimal shippingAmount, List<CorporateBudgetItem> items, String notes) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Orçamento corporativo deve conter pelo menos um item.");
        }
        BigDecimal calculatedSubtotal = items.stream()
                .map(CorporateBudgetItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disc = discount != null ? discount : BigDecimal.ZERO;
        BigDecimal ship = shippingAmount != null ? shippingAmount : BigDecimal.ZERO;
        BigDecimal calculatedTotal = calculatedSubtotal.subtract(disc).add(ship);

        return new CorporateBudget(UUID.randomUUID(), tenantId, budgetNumber, customerId,
                corporateName, tradeName, cnpj, stateRegistration, contactName, contactEmail,
                contactPhone, BudgetStatus.RASCUNHO, calculatedSubtotal, disc, ship,
                calculatedTotal, paymentTermsDays, null, notes, null, null, items,
                Instant.now(), Instant.now(), null);
    }

    public void submitForApproval() {
        if (this.status != BudgetStatus.RASCUNHO) {
            throw new BusinessException("Apenas orçamentos em rascunho podem ser enviados para aprovação.");
        }
        this.status = BudgetStatus.AGUARDANDO_APROVACAO;
        this.updatedAt = Instant.now();
    }

    /**
     * Aprovação com Trava de Inadimplência:
     * O cliente é terminantemente bloqueado de aprovar novos orçamentos
     * se possuir qualquer pedido faturado pendente e vencido até que ocorra a quitação integral.
     */
    public void approve(boolean hasOverdueInvoices) {
        if (hasOverdueInvoices) {
            throw new BusinessException("Aprovação bloqueada: O cliente possui faturas em aberto vencidas (Trava de Inadimplência ativada).");
        }
        if (this.status != BudgetStatus.AGUARDANDO_APROVACAO && this.status != BudgetStatus.RASCUNHO) {
            throw new BusinessException("Orçamento não está em estado para aprovação.");
        }
        this.status = BudgetStatus.APROVADO;
        this.approvedAt = Instant.now();
        this.dueDate = LocalDate.now().plusDays(this.paymentTermsDays);
        this.updatedAt = Instant.now();
    }

    public void reject(String reason) {
        if (this.status == BudgetStatus.FATURADO) {
            throw new BusinessException("Não é possível rejeitar um orçamento já faturado.");
        }
        this.status = BudgetStatus.REJEITADO;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markAsInvoiced(UUID orderId) {
        if (this.status != BudgetStatus.APROVADO) {
            throw new BusinessException("Apenas orçamentos aprovados podem ser faturados.");
        }
        this.status = BudgetStatus.FATURADO;
        this.invoicedOrderId = orderId;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == BudgetStatus.FATURADO) {
            throw new BusinessException("Não é possível cancelar um orçamento já faturado.");
        }
        this.status = BudgetStatus.CANCELADO;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getBudgetNumber() { return budgetNumber; }
    public UUID getCustomerId() { return customerId; }
    public String getCorporateName() { return corporateName; }
    public String getTradeName() { return tradeName; }
    public String getCnpj() { return cnpj; }
    public String getStateRegistration() { return stateRegistration; }
    public String getContactName() { return contactName; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public BudgetStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public int getPaymentTermsDays() { return paymentTermsDays; }
    public LocalDate getDueDate() { return dueDate; }
    public String getNotes() { return notes; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getInvoicedOrderId() { return invoicedOrderId; }
    public List<CorporateBudgetItem> getItems() { return Collections.unmodifiableList(items); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getApprovedAt() { return approvedAt; }
}
