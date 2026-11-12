package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.stock.AuditStatus;
import com.livraria.ecommerce.domain.model.stock.BlindAudit;
import com.livraria.ecommerce.domain.model.stock.BlindAuditItem;
import com.livraria.ecommerce.domain.port.out.BlindAuditRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.BlindAuditItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.BlindAuditJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataBlindAuditItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataBlindAuditRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
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
public class BlindAuditPersistenceAdapter implements BlindAuditRepositoryPort {

    private final SpringDataBlindAuditRepository auditRepository;
    private final SpringDataBlindAuditItemRepository auditItemRepository;
    private final SpringDataCompanyRepository companyRepository;
    private final SpringDataSkuRepository skuRepository;
    private final SpringDataProductRepository productRepository;

    public BlindAuditPersistenceAdapter(SpringDataBlindAuditRepository auditRepository,
                                        SpringDataBlindAuditItemRepository auditItemRepository,
                                        SpringDataCompanyRepository companyRepository,
                                        SpringDataSkuRepository skuRepository,
                                        SpringDataProductRepository productRepository) {
        this.auditRepository = auditRepository;
        this.auditItemRepository = auditItemRepository;
        this.companyRepository = companyRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public BlindAudit save(BlindAudit audit) {
        BlindAuditJpaEntity entity = BlindAuditJpaEntity.builder()
                .id(audit.getId())
                .tenantId(audit.getTenantId())
                .companyId(audit.getCompanyId())
                .transferId(audit.getTransferId())
                .code(audit.getCode())
                .status(audit.getStatus().name())
                .auditorId(audit.getAuditorId())
                .notes(audit.getNotes())
                .createdAt(audit.getCreatedAt())
                .completedAt(audit.getCompletedAt())
                .items(new ArrayList<>())
                .build();

        BlindAuditJpaEntity savedEntity = auditRepository.save(entity);

        // Delete existing items
        auditItemRepository.deleteAll(auditItemRepository.findAllByAuditId(audit.getId()));

        List<BlindAuditItemJpaEntity> itemEntities = new ArrayList<>();
        for (BlindAuditItem item : audit.getItems()) {
            BlindAuditItemJpaEntity itemEntity = BlindAuditItemJpaEntity.builder()
                    .id(item.getId())
                    .tenantId(audit.getTenantId())
                    .audit(savedEntity)
                    .skuId(item.getSkuId())
                    .quantityScanned(item.getQuantityScanned())
                    .quantityExpected(item.getQuantityExpected())
                    .divergence(item.getDivergence())
                    .lastScannedAt(item.getLastScannedAt())
                    .build();
            itemEntities.add(itemEntity);
        }
        auditItemRepository.saveAll(itemEntities);

        return findById(audit.getId()).orElse(audit);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlindAudit> findById(UUID id) {
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        Optional<BlindAuditJpaEntity> entityOpt = tenantId != null
                ? auditRepository.findByTenantIdAndId(tenantId, id)
                : auditRepository.findById(id);

        return entityOpt.map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BlindAudit> findByCode(UUID tenantId, String code) {
        return auditRepository.findByTenantIdAndCode(tenantId, code).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlindAudit> findAllByCompanyId(UUID tenantId, UUID companyId) {
        List<BlindAuditJpaEntity> entities = auditRepository.findAllByTenantIdAndCompanyIdOrderByCreatedAtDesc(tenantId, companyId);
        return entities.stream().map(this::toDomain).toList();
    }

    private BlindAudit toDomain(BlindAuditJpaEntity entity) {
        String companyName = companyRepository.findById(entity.getCompanyId())
                .map(CompanyJpaEntity::getTradeName).orElse("Filial " + entity.getCompanyId());

        List<BlindAuditItemJpaEntity> itemEntities = auditItemRepository.findAllByAuditId(entity.getId());
        List<UUID> skuIds = itemEntities.stream().map(BlindAuditItemJpaEntity::getSkuId).toList();

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

        List<BlindAuditItem> items = itemEntities.stream().map(i -> {
            SkuJpaEntity sku = skuMap.get(i.getSkuId());
            String skuCode = sku != null ? sku.getSkuCode() : i.getSkuId().toString();
            String title = productTitleMap.getOrDefault(i.getSkuId(), skuCode);

            return new BlindAuditItem(
                    i.getId(),
                    i.getTenantId(),
                    entity.getId(),
                    i.getSkuId(),
                    skuCode,
                    title,
                    i.getQuantityScanned(),
                    i.getQuantityExpected(),
                    i.getDivergence(),
                    i.getLastScannedAt()
            );
        }).toList();

        return new BlindAudit(
                entity.getId(),
                entity.getTenantId(),
                entity.getCompanyId(),
                companyName,
                entity.getTransferId(),
                entity.getCode(),
                AuditStatus.valueOf(entity.getStatus()),
                entity.getAuditorId(),
                entity.getNotes(),
                items,
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
