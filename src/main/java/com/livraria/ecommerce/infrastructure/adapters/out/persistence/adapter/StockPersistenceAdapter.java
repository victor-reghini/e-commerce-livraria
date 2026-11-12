package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockItemRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class StockPersistenceAdapter implements StockRepositoryPort {

    private final SpringDataStockItemRepository stockItemRepository;

    public StockPersistenceAdapter(SpringDataStockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Integer> findAvailableStockBySkuIds(UUID tenantId, UUID companyId, List<UUID> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<StockItemJpaEntity> items = stockItemRepository.findAllByTenantIdAndCompanyIdAndSkuIdIn(tenantId, companyId, skuIds);
        Map<UUID, Integer> result = new HashMap<>();
        for (StockItemJpaEntity item : items) {
            int available = Math.max(0, item.getQuantity() - item.getReservedQuantity());
            result.put(item.getSkuId(), available);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableStock(UUID tenantId, UUID companyId, UUID skuId) {
        return stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId)
                .map(item -> Math.max(0, item.getQuantity() - item.getReservedQuantity()))
                .orElse(0);
    }

    @Override
    @Transactional
    public void setStockQuantity(UUID tenantId, UUID companyId, UUID skuId, int quantity) {
        Optional<StockItemJpaEntity> existing = stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId);
        if (existing.isPresent()) {
            StockItemJpaEntity entity = existing.get();
            entity.setQuantity(quantity);
            entity.setUpdatedAt(Instant.now());
            stockItemRepository.save(entity);
        } else {
            StockItemJpaEntity entity = StockItemJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .companyId(companyId)
                    .skuId(skuId)
                    .quantity(quantity)
                    .reservedQuantity(0)
                    .minThreshold(0)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            stockItemRepository.save(entity);
        }
    }
}

