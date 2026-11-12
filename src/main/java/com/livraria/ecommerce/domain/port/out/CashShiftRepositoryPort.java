package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.pos.CashShift;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashShiftRepositoryPort {
    CashShift save(CashShift shift);
    Optional<CashShift> findById(UUID id);
    Optional<CashShift> findCurrentOpenShift(UUID tenantId, UUID companyId, UUID cashierId);
    List<CashShift> findAllByCompanyId(UUID tenantId, UUID companyId, int page, int size);
}

