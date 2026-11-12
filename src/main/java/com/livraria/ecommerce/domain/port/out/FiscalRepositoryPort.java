package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiscalRepositoryPort {

    FiscalInvoice save(FiscalInvoice invoice);

    Optional<FiscalInvoice> findById(UUID id);

    Optional<FiscalInvoice> findByAccessKey(String accessKey);

    Optional<FiscalInvoice> findByOrderId(UUID orderId);

    Optional<FiscalInvoice> findBySaleId(UUID saleId);

    long getNextInvoiceNumber(UUID tenantId, UUID companyId, String model, int series);

    List<FiscalInvoice> findAllByTenant(UUID tenantId, int page, int size);
}

