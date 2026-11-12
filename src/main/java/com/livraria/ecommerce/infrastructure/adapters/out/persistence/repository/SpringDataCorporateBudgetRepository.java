package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CorporateBudgetJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCorporateBudgetRepository extends JpaRepository<CorporateBudgetJpaEntity, UUID> {
    Optional<CorporateBudgetJpaEntity> findByTenantIdAndBudgetNumber(UUID tenantId, String budgetNumber);
    Page<CorporateBudgetJpaEntity> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    List<CorporateBudgetJpaEntity> findAllByTenantIdAndCnpjOrderByCreatedAtDesc(UUID tenantId, String cnpj);
}

