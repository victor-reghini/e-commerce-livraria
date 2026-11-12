package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorporateBudgetRepositoryPort {
    Optional<CorporateBudget> findById(UUID id);
    Optional<CorporateBudget> findByTenantIdAndBudgetNumber(UUID tenantId, String budgetNumber);
    List<CorporateBudget> findAllByTenantId(UUID tenantId, int page, int size);
    List<CorporateBudget> findAllByTenantIdAndCnpj(UUID tenantId, String cnpj);
    CorporateBudget save(CorporateBudget budget);
}
