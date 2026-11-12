package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import com.livraria.ecommerce.domain.model.fiscal.FiscalModel;
import com.livraria.ecommerce.domain.model.fiscal.FiscalStatus;
import com.livraria.ecommerce.domain.port.out.FiscalRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.FiscalInvoiceJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.FiscalNumberingJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataFiscalInvoiceRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataFiscalNumberingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FiscalPersistenceAdapter implements FiscalRepositoryPort {

    private final SpringDataFiscalInvoiceRepository invoiceRepository;
    private final SpringDataFiscalNumberingRepository numberingRepository;

    public FiscalPersistenceAdapter(SpringDataFiscalInvoiceRepository invoiceRepository,
                                    SpringDataFiscalNumberingRepository numberingRepository) {
        this.invoiceRepository = invoiceRepository;
        this.numberingRepository = numberingRepository;
    }

    @Override
    @Transactional
    public FiscalInvoice save(FiscalInvoice invoice) {
        if (invoice.getId() == null) {
            invoice.setId(UUID.randomUUID());
        }
        FiscalInvoiceJpaEntity entity = toEntity(invoice);
        FiscalInvoiceJpaEntity saved = invoiceRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FiscalInvoice> findById(UUID id) {
        return invoiceRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FiscalInvoice> findByAccessKey(String accessKey) {
        return invoiceRepository.findByAccessKey(accessKey).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FiscalInvoice> findByOrderId(UUID orderId) {
        return invoiceRepository.findByOrderId(orderId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FiscalInvoice> findBySaleId(UUID saleId) {
        return invoiceRepository.findBySaleId(saleId).map(this::toDomain);
    }

    @Override
    @Transactional
    public long getNextInvoiceNumber(UUID tenantId, UUID companyId, String model, int series) {
        Optional<FiscalNumberingJpaEntity> sequenceOpt = numberingRepository
                .findByTenantIdAndCompanyIdAndModelAndSeriesForUpdate(tenantId, companyId, model, series);

        FiscalNumberingJpaEntity sequence;
        if (sequenceOpt.isPresent()) {
            sequence = sequenceOpt.get();
            sequence.setCurrentNumber(sequence.getCurrentNumber() + 1);
            sequence.setUpdatedAt(Instant.now());
        } else {
            sequence = FiscalNumberingJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .companyId(companyId)
                    .model(model)
                    .series(series)
                    .currentNumber(1L)
                    .updatedAt(Instant.now())
                    .build();
        }
        numberingRepository.save(sequence);
        return sequence.getCurrentNumber();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiscalInvoice> findAllByTenant(UUID tenantId, int page, int size) {
        return invoiceRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private FiscalInvoiceJpaEntity toEntity(FiscalInvoice invoice) {
        return FiscalInvoiceJpaEntity.builder()
                .id(invoice.getId())
                .tenantId(invoice.getTenantId())
                .companyId(invoice.getCompanyId())
                .orderId(invoice.getOrderId())
                .saleId(invoice.getSaleId())
                .model(invoice.getModel() != null ? invoice.getModel().getCode() : "55")
                .series(invoice.getSeries())
                .invoiceNumber(invoice.getInvoiceNumber())
                .accessKey(invoice.getAccessKey())
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : FiscalStatus.EMITIDA.name())
                .natureOfOperation(invoice.getNatureOfOperation())
                .totalAmount(invoice.getTotalAmount())
                .totalDiscount(invoice.getTotalDiscount())
                .totalTaxIcms(invoice.getTotalTaxIcms())
                .totalTaxPis(invoice.getTotalTaxPis())
                .totalTaxCofins(invoice.getTotalTaxCofins())
                .protocolNumber(invoice.getProtocolNumber())
                .xmlPayload(invoice.getXmlPayload())
                .danfeUrl(invoice.getDanfeUrl())
                .rejectionReason(invoice.getRejectionReason())
                .cancellationReason(invoice.getCancellationReason())
                .createdAt(invoice.getCreatedAt() != null ? invoice.getCreatedAt() : Instant.now())
                .authorizedAt(invoice.getAuthorizedAt())
                .canceledAt(invoice.getCanceledAt())
                .build();
    }

    private FiscalInvoice toDomain(FiscalInvoiceJpaEntity entity) {
        FiscalInvoice invoice = new FiscalInvoice();
        invoice.setId(entity.getId());
        invoice.setTenantId(entity.getTenantId());
        invoice.setCompanyId(entity.getCompanyId());
        invoice.setOrderId(entity.getOrderId());
        invoice.setSaleId(entity.getSaleId());
        invoice.setModel(FiscalModel.fromCode(entity.getModel()));
        invoice.setSeries(entity.getSeries());
        invoice.setInvoiceNumber(entity.getInvoiceNumber());
        invoice.setAccessKey(entity.getAccessKey());
        invoice.setStatus(FiscalStatus.valueOf(entity.getStatus()));
        invoice.setNatureOfOperation(entity.getNatureOfOperation());
        invoice.setTotalAmount(entity.getTotalAmount());
        invoice.setTotalDiscount(entity.getTotalDiscount());
        invoice.setTotalTaxIcms(entity.getTotalTaxIcms());
        invoice.setTotalTaxPis(entity.getTotalTaxPis());
        invoice.setTotalTaxCofins(entity.getTotalTaxCofins());
        invoice.setProtocolNumber(entity.getProtocolNumber());
        invoice.setXmlPayload(entity.getXmlPayload());
        invoice.setDanfeUrl(entity.getDanfeUrl());
        invoice.setRejectionReason(entity.getRejectionReason());
        invoice.setCancellationReason(entity.getCancellationReason());
        invoice.setCreatedAt(entity.getCreatedAt());
        invoice.setAuthorizedAt(entity.getAuthorizedAt());
        invoice.setCanceledAt(entity.getCanceledAt());
        return invoice;
    }
}

