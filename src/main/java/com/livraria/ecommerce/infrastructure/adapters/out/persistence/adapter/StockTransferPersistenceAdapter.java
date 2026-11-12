package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.stock.StockTransfer;
import com.livraria.ecommerce.domain.model.stock.StockTransferItem;
import com.livraria.ecommerce.domain.model.stock.TransferStatus;
import com.livraria.ecommerce.domain.model.stock.TransferType;
import com.livraria.ecommerce.domain.port.out.StockTransferRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockTransferItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockTransferJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockTransferItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockTransferRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class StockTransferPersistenceAdapter implements StockTransferRepositoryPort {

    private final SpringDataStockTransferRepository transferRepository;
    private final SpringDataStockTransferItemRepository transferItemRepository;
    private final SpringDataCompanyRepository companyRepository;
    private final SpringDataSkuRepository skuRepository;
    private final SpringDataProductRepository productRepository;

    public StockTransferPersistenceAdapter(SpringDataStockTransferRepository transferRepository,
                                           SpringDataStockTransferItemRepository transferItemRepository,
                                           SpringDataCompanyRepository companyRepository,
                                           SpringDataSkuRepository skuRepository,
                                           SpringDataProductRepository productRepository) {
        this.transferRepository = transferRepository;
        this.transferItemRepository = transferItemRepository;
        this.companyRepository = companyRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public StockTransfer save(StockTransfer transfer) {
        StockTransferJpaEntity entity = StockTransferJpaEntity.builder()
                .id(transfer.getId())
                .tenantId(transfer.getTenantId())
                .transferNumber(transfer.getTransferNumber())
                .originCompanyId(transfer.getOriginCompanyId())
                .destinationCompanyId(transfer.getDestinationCompanyId())
                .status(transfer.getStatus().name())
                .transferType(transfer.getTransferType().name())
                .notes(transfer.getNotes())
                .dispatchedAt(transfer.getDispatchedAt())
                .receivedAt(transfer.getReceivedAt())
                .createdBy(transfer.getCreatedBy())
                .receivedBy(transfer.getReceivedBy())
                .createdAt(transfer.getCreatedAt())
                .updatedAt(transfer.getUpdatedAt())
                .items(new ArrayList<>())
                .build();

        StockTransferJpaEntity savedEntity = transferRepository.save(entity);

        // Delete previous items if updating
        transferItemRepository.deleteAll(transferItemRepository.findAllByTransferId(transfer.getId()));

        List<StockTransferItemJpaEntity> itemEntities = new ArrayList<>();
        for (StockTransferItem item : transfer.getItems()) {
            StockTransferItemJpaEntity itemEntity = StockTransferItemJpaEntity.builder()
                    .id(item.getId())
                    .tenantId(transfer.getTenantId())
                    .transfer(savedEntity)
                    .skuId(item.getSkuId())
                    .quantityRequested(item.getQuantityRequested())
                    .quantityDispatched(item.getQuantityDispatched())
                    .quantityReceived(item.getQuantityReceived())
                    .unitCost(item.getUnitCost())
                    .unitPrice(item.getUnitPrice())
                    .createdAt(transfer.getCreatedAt())
                    .build();
            itemEntities.add(itemEntity);
        }
        transferItemRepository.saveAll(itemEntities);

        return findById(transfer.getId()).orElse(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockTransfer> findById(UUID id) {
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        Optional<StockTransferJpaEntity> entityOpt = tenantId != null
                ? transferRepository.findByTenantIdAndId(tenantId, id)
                : transferRepository.findById(id);

        return entityOpt.map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockTransfer> findByTransferNumber(UUID tenantId, String transferNumber) {
        return transferRepository.findByTenantIdAndTransferNumber(tenantId, transferNumber).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransfer> findAllByCompanyId(UUID tenantId, UUID companyId) {
        List<StockTransferJpaEntity> entities = transferRepository.findAllByCompany(tenantId, companyId);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransfer> findAllByTenantId(UUID tenantId) {
        List<StockTransferJpaEntity> entities = transferRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
        return entities.stream().map(this::toDomain).toList();
    }

    private StockTransfer toDomain(StockTransferJpaEntity entity) {
        String originName = companyRepository.findById(entity.getOriginCompanyId())
                .map(CompanyJpaEntity::getTradeName).orElse("Filial " + entity.getOriginCompanyId());
        String destName = companyRepository.findById(entity.getDestinationCompanyId())
                .map(CompanyJpaEntity::getTradeName).orElse("Filial " + entity.getDestinationCompanyId());

        List<StockTransferItemJpaEntity> itemEntities = transferItemRepository.findAllByTransferId(entity.getId());
        List<UUID> skuIds = itemEntities.stream().map(StockTransferItemJpaEntity::getSkuId).toList();

        Map<UUID, SkuJpaEntity> skuMap = new HashMap<>();
        Map<UUID, String> productTitleMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<SkuJpaEntity> skus = skuRepository.findAllByIdIn(skuIds);
            for (SkuJpaEntity sku : skus) {
                skuMap.put(sku.getId(), sku);
                productRepository.findById(sku.getProductId())
                        .ifPresent(p -> productTitleMap.put(sku.getId(), p.getTitle()));
            }
        }

        List<StockTransferItem> items = itemEntities.stream().map(i -> {
            SkuJpaEntity sku = skuMap.get(i.getSkuId());
            String skuCode = sku != null ? sku.getSkuCode() : i.getSkuId().toString();
            String title = productTitleMap.getOrDefault(i.getSkuId(), skuCode);

            return new StockTransferItem(
                    i.getId(),
                    i.getTenantId(),
                    entity.getId(),
                    i.getSkuId(),
                    skuCode,
                    title,
                    i.getQuantityRequested(),
                    i.getQuantityDispatched(),
                    i.getQuantityReceived(),
                    i.getUnitCost(),
                    i.getUnitPrice()
            );
        }).toList();

        return new StockTransfer(
                entity.getId(),
                entity.getTenantId(),
                entity.getTransferNumber(),
                entity.getOriginCompanyId(),
                originName,
                entity.getDestinationCompanyId(),
                destName,
                TransferStatus.valueOf(entity.getStatus()),
                TransferType.valueOf(entity.getTransferType()),
                entity.getNotes(),
                items,
                entity.getDispatchedAt(),
                entity.getReceivedAt(),
                entity.getCreatedBy(),
                entity.getReceivedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
