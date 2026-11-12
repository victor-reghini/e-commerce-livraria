package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.pos.NfceStatus;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.PosSalePayment;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import com.livraria.ecommerce.domain.port.out.PosSaleRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.PosSaleItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.PosSaleJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.PosSalePaymentJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataPosSaleItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataPosSalePaymentRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataPosSaleRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class PosSalePersistenceAdapter implements PosSaleRepositoryPort {

    private final SpringDataPosSaleRepository saleRepository;
    private final SpringDataPosSaleItemRepository itemRepository;
    private final SpringDataPosSalePaymentRepository paymentRepository;
    private final SpringDataSkuRepository skuRepository;
    private final SpringDataProductRepository productRepository;

    public PosSalePersistenceAdapter(SpringDataPosSaleRepository saleRepository,
                                    SpringDataPosSaleItemRepository itemRepository,
                                    SpringDataPosSalePaymentRepository paymentRepository,
                                    SpringDataSkuRepository skuRepository,
                                    SpringDataProductRepository productRepository) {
        this.saleRepository = saleRepository;
        this.itemRepository = itemRepository;
        this.paymentRepository = paymentRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public PosSale save(PosSale sale) {
        PosSaleJpaEntity entity = PosSaleJpaEntity.builder()
                .id(sale.getId())
                .tenantId(sale.getTenantId())
                .companyId(sale.getCompanyId())
                .shiftId(sale.getShiftId())
                .saleNumber(sale.getSaleNumber())
                .clientUuid(sale.getClientUuid())
                .customerName(sale.getCustomerName())
                .customerCpfCnpj(sale.getCustomerCpfCnpj())
                .subtotal(sale.getSubtotal())
                .discount(sale.getDiscount())
                .total(sale.getTotal())
                .totalPaid(sale.getTotalPaid())
                .changeAmount(sale.getChangeAmount())
                .status(sale.getStatus().name())
                .nfceKey(sale.getNfceKey())
                .nfceXml(sale.getNfceXml())
                .nfceStatus(sale.getNfceStatus().name())
                .createdBy(sale.getCreatedBy())
                .createdAt(sale.getCreatedAt())
                .updatedAt(sale.getUpdatedAt())
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        PosSaleJpaEntity savedEntity = saleRepository.save(entity);

        // Delete existing items & payments if updating
        itemRepository.deleteAll(itemRepository.findAllBySaleId(sale.getId()));
        paymentRepository.deleteAll(paymentRepository.findAllBySaleId(sale.getId()));

        List<PosSaleItemJpaEntity> itemEntities = new ArrayList<>();
        for (PosSaleItem item : sale.getItems()) {
            itemEntities.add(PosSaleItemJpaEntity.builder()
                    .id(item.getId())
                    .tenantId(sale.getTenantId())
                    .sale(savedEntity)
                    .skuId(item.getSkuId())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .discount(item.getDiscount())
                    .subtotal(item.getSubtotal())
                    .createdAt(sale.getCreatedAt())
                    .build());
        }
        itemRepository.saveAll(itemEntities);

        List<PosSalePaymentJpaEntity> paymentEntities = new ArrayList<>();
        for (PosSalePayment payment : sale.getPayments()) {
            paymentEntities.add(PosSalePaymentJpaEntity.builder()
                    .id(payment.getId())
                    .tenantId(sale.getTenantId())
                    .sale(savedEntity)
                    .paymentMethod(payment.getPaymentMethod().name())
                    .amount(payment.getAmount())
                    .createdAt(sale.getCreatedAt())
                    .build());
        }
        paymentRepository.saveAll(paymentEntities);

        return findById(sale.getId()).orElse(sale);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PosSale> findById(UUID id) {
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        Optional<PosSaleJpaEntity> entityOpt = tenantId != null
                ? saleRepository.findByTenantIdAndId(tenantId, id)
                : saleRepository.findById(id);

        return entityOpt.map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PosSale> findByClientUuid(UUID tenantId, UUID clientUuid) {
        return saleRepository.findByTenantIdAndClientUuid(tenantId, clientUuid).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PosSale> findBySaleNumber(UUID tenantId, String saleNumber) {
        return saleRepository.findByTenantIdAndSaleNumber(tenantId, saleNumber).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosSale> findAllByShiftId(UUID shiftId) {
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        List<PosSaleJpaEntity> entities = saleRepository.findAllByTenantIdAndShiftIdOrderByCreatedAtDesc(tenantId, shiftId);
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PosSale> findAllByCompanyId(UUID tenantId, UUID companyId, int page, int size) {
        List<PosSaleJpaEntity> entities = saleRepository.findAllByTenantIdAndCompanyIdOrderByCreatedAtDesc(
                tenantId, companyId, PageRequest.of(Math.max(0, page), Math.max(1, size))
        );
        return entities.stream().map(this::toDomain).toList();
    }

    private PosSale toDomain(PosSaleJpaEntity entity) {
        List<PosSaleItemJpaEntity> itemEntities = itemRepository.findAllBySaleId(entity.getId());
        List<UUID> skuIds = itemEntities.stream().map(PosSaleItemJpaEntity::getSkuId).toList();

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

        List<PosSaleItem> items = itemEntities.stream().map(i -> {
            SkuJpaEntity sku = skuMap.get(i.getSkuId());
            String skuCode = sku != null ? sku.getSkuCode() : i.getSkuId().toString();
            String title = productTitleMap.getOrDefault(i.getSkuId(), skuCode);

            return new PosSaleItem(
                    i.getId(),
                    i.getTenantId(),
                    entity.getId(),
                    i.getSkuId(),
                    skuCode,
                    title,
                    i.getQuantity(),
                    i.getUnitPrice(),
                    i.getDiscount(),
                    i.getSubtotal()
            );
        }).toList();

        List<PosSalePaymentJpaEntity> paymentEntities = paymentRepository.findAllBySaleId(entity.getId());
        List<PosSalePayment> payments = paymentEntities.stream().map(p -> new PosSalePayment(
                p.getId(),
                p.getTenantId(),
                entity.getId(),
                PaymentMethod.valueOf(p.getPaymentMethod()),
                p.getAmount()
        )).toList();

        return new PosSale(
                entity.getId(),
                entity.getTenantId(),
                entity.getCompanyId(),
                entity.getShiftId(),
                entity.getSaleNumber(),
                entity.getClientUuid(),
                entity.getCustomerName(),
                entity.getCustomerCpfCnpj(),
                entity.getSubtotal(),
                entity.getDiscount(),
                entity.getTotal(),
                entity.getTotalPaid(),
                entity.getChangeAmount(),
                SaleStatus.valueOf(entity.getStatus()),
                entity.getNfceKey(),
                entity.getNfceXml(),
                NfceStatus.valueOf(entity.getNfceStatus()),
                items,
                payments,
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
