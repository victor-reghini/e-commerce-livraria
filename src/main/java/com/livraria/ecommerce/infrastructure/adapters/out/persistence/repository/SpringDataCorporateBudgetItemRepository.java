package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CorporateBudgetItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataCorporateBudgetItemRepository extends JpaRepository<CorporateBudgetItemJpaEntity, UUID> {
    List<CorporateBudgetItemJpaEntity> findAllByTenantIdAndBudgetId(UUID tenantId, UUID budgetId);
}

