package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.stock.BlindAudit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlindAuditRepositoryPort {
    BlindAudit save(BlindAudit audit);
    Optional<BlindAudit> findById(UUID id);
    Optional<BlindAudit> findByCode(UUID tenantId, String code);
    List<BlindAudit> findAllByCompanyId(UUID tenantId, UUID companyId);
}

