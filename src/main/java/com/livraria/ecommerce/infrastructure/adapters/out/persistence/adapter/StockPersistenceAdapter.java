package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.exception.BusinessException;
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
    @Transactional(readOnly = true)
    public int getPhysicalStock(UUID tenantId, UUID companyId, UUID skuId) {
        return stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId)
                .map(StockItemJpaEntity::getQuantity)
                .orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalPhysicalStock(UUID tenantId, UUID companyId) {
        return stockItemRepository.sumQuantityByTenantIdAndCompanyId(tenantId, companyId);
    }

    @Override
    @Transactional
    public void setStockQuantity(UUID tenantId, UUID companyId, UUID skuId, int quantity) {
        stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId)
                .ifPresentOrElse(
                        entity -> {
                            entity.setQuantity(quantity);
                            entity.setUpdatedAt(Instant.now());
                            stockItemRepository.save(entity);
                        },
                        () -> {
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
                );
    }

    @Override
    @Transactional
    public void reserveStock(UUID tenantId, UUID companyId, UUID skuId, int quantity) {
        StockItemJpaEntity entity = stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId)
                .orElseThrow(() -> new BusinessException("Item SKU " + skuId + " não encontrado no estoque da filial de origem."));

        int available = entity.getQuantity() - entity.getReservedQuantity();
        if (available < quantity) {
            throw new BusinessException("Estoque disponível insuficiente para o SKU " + skuId + ". Disponível: " + available + ", Solicitado: " + quantity);
        }

        entity.setReservedQuantity(entity.getReservedQuantity() + quantity);
        entity.setUpdatedAt(Instant.now());
        stockItemRepository.save(entity);
    }

    @Override
    @Transactional
    public void releaseStockReservation(UUID tenantId, UUID companyId, UUID skuId, int quantity) {
        stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId).ifPresent(entity -> {
            entity.setReservedQuantity(Math.max(0, entity.getReservedQuantity() - quantity));
            entity.setUpdatedAt(Instant.now());
            stockItemRepository.save(entity);
        });
    }

    @Override
    @Transactional
    public void transferStock(UUID tenantId, UUID originCompanyId, UUID destinationCompanyId, UUID skuId, int quantity) {
        // Decrease from origin
        StockItemJpaEntity origin = stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, originCompanyId, skuId)
                .orElseThrow(() -> new BusinessException("Item SKU " + skuId + " não encontrado no estoque de origem."));

        if (origin.getQuantity() < quantity) {
            throw new BusinessException("Estoque físico insuficiente no estoque de origem para SKU " + skuId);
        }

        origin.setQuantity(origin.getQuantity() - quantity);
        origin.setReservedQuantity(Math.max(0, origin.getReservedQuantity() - quantity));
        origin.setUpdatedAt(Instant.now());
        stockItemRepository.save(origin);

        // Increase at destination
        stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, destinationCompanyId, skuId)
                .ifPresentOrElse(
                        dest -> {
                            dest.setQuantity(dest.getQuantity() + quantity);
                            dest.setUpdatedAt(Instant.now());
                            stockItemRepository.save(dest);
                        },
                        () -> {
                            StockItemJpaEntity dest = StockItemJpaEntity.builder()
                                    .id(UUID.randomUUID())
                                    .tenantId(tenantId)
                                    .companyId(destinationCompanyId)
                                    .skuId(skuId)
                                    .quantity(quantity)
                                    .reservedQuantity(0)
                                    .minThreshold(0)
                                    .createdAt(Instant.now())
                                    .updatedAt(Instant.now())
                                    .build();
                            stockItemRepository.save(dest);
                        }
                );
    }
}
