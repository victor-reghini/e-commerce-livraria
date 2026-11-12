package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.FiscalInvoiceJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataFiscalInvoiceRepository extends JpaRepository<FiscalInvoiceJpaEntity, UUID> {

    Optional<FiscalInvoiceJpaEntity> findByAccessKey(String accessKey);

    Optional<FiscalInvoiceJpaEntity> findByOrderId(UUID orderId);

    Optional<FiscalInvoiceJpaEntity> findBySaleId(UUID saleId);

    Page<FiscalInvoiceJpaEntity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}

