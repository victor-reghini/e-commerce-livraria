package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.stock.MovementType;
import com.livraria.ecommerce.domain.model.stock.StockMovement;
import com.livraria.ecommerce.domain.port.out.StockMovementRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockMovementJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockMovementRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class StockMovementPersistenceAdapter implements StockMovementRepositoryPort {

    private final SpringDataStockMovementRepository movementRepository;

    public StockMovementPersistenceAdapter(SpringDataStockMovementRepository movementRepository) {
        this.movementRepository = movementRepository;
    }

    @Override
    @Transactional
    public StockMovement save(StockMovement movement) {
        StockMovementJpaEntity entity = StockMovementJpaEntity.builder()
                .id(movement.getId())
                .tenantId(movement.getTenantId())
                .companyId(movement.getCompanyId())
                .skuId(movement.getSkuId())
                .movementType(movement.getMovementType().name())
                .quantityChange(movement.getQuantityChange())
                .balanceAfter(movement.getBalanceAfter())
                .referenceId(movement.getReferenceId())
                .reason(movement.getReason())
                .createdBy(movement.getCreatedBy())
                .createdAt(movement.getCreatedAt())
                .build();

        StockMovementJpaEntity saved = movementRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovement> findAllBySkuId(UUID tenantId, UUID companyId, UUID skuId) {
        List<StockMovementJpaEntity> entities = movementRepository.findAllByTenantIdAndCompanyIdAndSkuIdOrderByCreatedAtDesc(tenantId, companyId, skuId);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockMovement> findAllByCompanyId(UUID tenantId, UUID companyId, int page, int size) {
        List<StockMovementJpaEntity> entities = movementRepository.findAllByTenantIdAndCompanyIdOrderByCreatedAtDesc(
                tenantId, companyId, PageRequest.of(Math.max(0, page), Math.max(1, size))
        );
        return entities.stream().map(this::toDomain).toList();
    }

    private StockMovement toDomain(StockMovementJpaEntity entity) {
        return new StockMovement(
                entity.getId(),
                entity.getTenantId(),
                entity.getCompanyId(),
                entity.getSkuId(),
                MovementType.valueOf(entity.getMovementType()),
                entity.getQuantityChange(),
                entity.getBalanceAfter(),
                entity.getReferenceId(),
                entity.getReason(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }
}
