package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.pos.PosSale;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosSaleRepositoryPort {
    PosSale save(PosSale sale);
    Optional<PosSale> findById(UUID id);
    Optional<PosSale> findByClientUuid(UUID tenantId, UUID clientUuid);
    Optional<PosSale> findBySaleNumber(UUID tenantId, String saleNumber);
    List<PosSale> findAllByShiftId(UUID shiftId);
    List<PosSale> findAllByCompanyId(UUID tenantId, UUID companyId, int page, int size);
}

