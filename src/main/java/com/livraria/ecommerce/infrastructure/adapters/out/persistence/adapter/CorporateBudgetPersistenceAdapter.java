package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.ecommerce.BudgetStatus;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudget;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudgetItem;
import com.livraria.ecommerce.domain.port.out.CorporateBudgetRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CorporateBudgetItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CorporateBudgetJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCorporateBudgetItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCorporateBudgetRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CorporateBudgetPersistenceAdapter implements CorporateBudgetRepositoryPort {

    private final SpringDataCorporateBudgetRepository budgetRepository;
    private final SpringDataCorporateBudgetItemRepository itemRepository;

    public CorporateBudgetPersistenceAdapter(SpringDataCorporateBudgetRepository budgetRepository,
                                             SpringDataCorporateBudgetItemRepository itemRepository) {
        this.budgetRepository = budgetRepository;
        this.itemRepository = itemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CorporateBudget> findById(UUID id) {
        return budgetRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CorporateBudget> findByTenantIdAndBudgetNumber(UUID tenantId, String budgetNumber) {
        return budgetRepository.findByTenantIdAndBudgetNumber(tenantId, budgetNumber).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorporateBudget> findAllByTenantId(UUID tenantId, int page, int size) {
        return budgetRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorporateBudget> findAllByTenantIdAndCnpj(UUID tenantId, String cnpj) {
        String cleanCnpj = cnpj != null ? cnpj.replaceAll("\\D", "") : "";
        return budgetRepository.findAllByTenantIdAndCnpjOrderByCreatedAtDesc(tenantId, cleanCnpj)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public CorporateBudget save(CorporateBudget budget) {
        CorporateBudgetJpaEntity entity = CorporateBudgetJpaEntity.builder()
                .id(budget.getId())
                .tenantId(budget.getTenantId())
                .budgetNumber(budget.getBudgetNumber())
                .customerId(budget.getCustomerId())
                .corporateName(budget.getCorporateName())
                .tradeName(budget.getTradeName())
                .cnpj(budget.getCnpj())
                .stateRegistration(budget.getStateRegistration())
                .contactName(budget.getContactName())
                .contactEmail(budget.getContactEmail())
                .contactPhone(budget.getContactPhone())
                .status(budget.getStatus().name())
                .subtotal(budget.getSubtotal())
                .discount(budget.getDiscount())
                .shippingAmount(budget.getShippingAmount())
                .totalAmount(budget.getTotalAmount())
                .paymentTermsDays(budget.getPaymentTermsDays())
                .dueDate(budget.getDueDate())
                .notes(budget.getNotes())
                .rejectionReason(budget.getRejectionReason())
                .invoicedOrderId(budget.getInvoicedOrderId())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .approvedAt(budget.getApprovedAt())
                .build();

        budgetRepository.save(entity);

        for (CorporateBudgetItem item : budget.getItems()) {
            CorporateBudgetItemJpaEntity itemEntity = CorporateBudgetItemJpaEntity.builder()
                    .id(item.getId())
                    .tenantId(budget.getTenantId())
                    .budget(entity)
                    .skuId(item.getSkuId())
                    .skuCode(item.getSkuCode())
                    .productTitle(item.getProductTitle())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .discount(item.getDiscount())
                    .total(item.getTotal())
                    .build();
            itemRepository.save(itemEntity);
        }

        return budget;
    }

    private CorporateBudget toDomain(CorporateBudgetJpaEntity entity) {
        List<CorporateBudgetItemJpaEntity> itemEntities = itemRepository.findAllByTenantIdAndBudgetId(entity.getTenantId(), entity.getId());
        List<CorporateBudgetItem> domainItems = itemEntities.stream().map(i -> new CorporateBudgetItem(
                i.getId(),
                i.getSkuId(),
                i.getSkuCode(),
                i.getProductTitle(),
                i.getQuantity(),
                i.getUnitPrice(),
                i.getDiscount(),
                i.getTotal()
        )).toList();

        return new CorporateBudget(
                entity.getId(),
                entity.getTenantId(),
                entity.getBudgetNumber(),
                entity.getCustomerId(),
                entity.getCorporateName(),
                entity.getTradeName(),
                entity.getCnpj(),
                entity.getStateRegistration(),
                entity.getContactName(),
                entity.getContactEmail(),
                entity.getContactPhone(),
                BudgetStatus.valueOf(entity.getStatus()),
                entity.getSubtotal(),
                entity.getDiscount(),
                entity.getShippingAmount(),
                entity.getTotalAmount(),
                entity.getPaymentTermsDays(),
                entity.getDueDate(),
                entity.getNotes(),
                entity.getRejectionReason(),
                entity.getInvoicedOrderId(),
                domainItems,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getApprovedAt()
        );
    }
}
