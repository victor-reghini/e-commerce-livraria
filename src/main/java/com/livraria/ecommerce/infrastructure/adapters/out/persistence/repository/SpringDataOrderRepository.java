package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {

    Optional<OrderJpaEntity> findByTenantIdAndOrderNumber(UUID tenantId, String orderNumber);

    List<OrderJpaEntity> findAllByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    Page<OrderJpaEntity> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    List<OrderJpaEntity> findAllByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status);

    /**
     * Trava de Inadimplência:
     * Retorna true se houver qualquer parcela/boleto não pago cuja data de vencimento seja anterior a hoje.
     */
    @Query("""
        SELECT COUNT(p) > 0
        FROM OrderPaymentJpaEntity p
        JOIN p.order o
        WHERE o.tenantId = :tenantId
          AND o.customerDocument = :document
          AND p.status != 'PAGO'
          AND p.boletoDueDate IS NOT NULL
          AND p.boletoDueDate < :today
    """)
    boolean hasOverdueInvoices(@Param("tenantId") UUID tenantId,
                               @Param("document") String document,
                               @Param("today") LocalDate today);
}

