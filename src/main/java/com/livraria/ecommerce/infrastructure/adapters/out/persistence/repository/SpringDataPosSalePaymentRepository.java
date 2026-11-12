package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.PosSalePaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataPosSalePaymentRepository extends JpaRepository<PosSalePaymentJpaEntity, UUID> {
    List<PosSalePaymentJpaEntity> findAllBySaleId(UUID saleId);
}
