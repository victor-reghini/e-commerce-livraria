package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.stock.StockTransfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockTransferRepositoryPort {
    StockTransfer save(StockTransfer transfer);
    Optional<StockTransfer> findById(UUID id);
    Optional<StockTransfer> findByTransferNumber(UUID tenantId, String transferNumber);
    List<StockTransfer> findAllByCompanyId(UUID tenantId, UUID companyId);
    List<StockTransfer> findAllByTenantId(UUID tenantId);
}

