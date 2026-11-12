package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.BlindAuditItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataBlindAuditItemRepository extends JpaRepository<BlindAuditItemJpaEntity, UUID> {
    List<BlindAuditItemJpaEntity> findAllByAuditId(UUID auditId);
    Optional<BlindAuditItemJpaEntity> findByAuditIdAndSkuId(UUID auditId, UUID skuId);
}

