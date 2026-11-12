package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CashMovementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataCashMovementRepository extends JpaRepository<CashMovementJpaEntity, UUID> {
    List<CashMovementJpaEntity> findAllByShiftIdOrderByCreatedAtAsc(UUID shiftId);
}

