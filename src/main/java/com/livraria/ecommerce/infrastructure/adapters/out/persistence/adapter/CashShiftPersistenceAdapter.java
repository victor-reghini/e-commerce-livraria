package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.pos.CashMovement;
import com.livraria.ecommerce.domain.model.pos.CashMovementType;
import com.livraria.ecommerce.domain.model.pos.CashShift;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.ShiftStatus;
import com.livraria.ecommerce.domain.port.out.CashShiftRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CashMovementJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CashShiftJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCashMovementRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCashShiftRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataUserRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CashShiftPersistenceAdapter implements CashShiftRepositoryPort {

    private final SpringDataCashShiftRepository shiftRepository;
    private final SpringDataCashMovementRepository movementRepository;
    private final SpringDataUserRepository userRepository;

    public CashShiftPersistenceAdapter(SpringDataCashShiftRepository shiftRepository,
                                       SpringDataCashMovementRepository movementRepository,
                                       SpringDataUserRepository userRepository) {
        this.shiftRepository = shiftRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public CashShift save(CashShift shift) {
        CashShiftJpaEntity entity = CashShiftJpaEntity.builder()
                .id(shift.getId())
                .tenantId(shift.getTenantId())
                .companyId(shift.getCompanyId())
                .cashierId(shift.getCashierId())
                .shiftNumber(shift.getShiftNumber())
                .status(shift.getStatus().name())
                .openingBalance(shift.getOpeningBalance())
                .expectedBalance(shift.getExpectedBalance())
                .actualBalance(shift.getActualBalance())
                .divergence(shift.getDivergence())
                .notes(shift.getNotes())
                .openedAt(shift.getOpenedAt())
                .closedAt(shift.getClosedAt())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt())
                .movements(new ArrayList<>())
                .build();

        CashShiftJpaEntity savedEntity = shiftRepository.save(entity);

        // Delete existing movements if updating
        movementRepository.deleteAll(movementRepository.findAllByShiftIdOrderByCreatedAtAsc(shift.getId()));

        List<CashMovementJpaEntity> movementEntities = new ArrayList<>();
        for (CashMovement m : shift.getMovements()) {
            movementEntities.add(CashMovementJpaEntity.builder()
                    .id(m.getId())
                    .tenantId(shift.getTenantId())
                    .shift(savedEntity)
                    .movementType(m.getMovementType().name())
                    .amount(m.getAmount())
                    .paymentMethod(m.getPaymentMethod().name())
                    .reason(m.getReason())
                    .operatorId(m.getOperatorId())
                    .createdAt(m.getCreatedAt())
                    .build());
        }
        movementRepository.saveAll(movementEntities);

        return findById(shift.getId()).orElse(shift);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashShift> findById(UUID id) {
        UUID tenantId = TenantContext.getTenantId().orElse(null);
        Optional<CashShiftJpaEntity> entityOpt = tenantId != null
                ? shiftRepository.findByTenantIdAndId(tenantId, id)
                : shiftRepository.findById(id);

        return entityOpt.map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CashShift> findCurrentOpenShift(UUID tenantId, UUID companyId, UUID cashierId) {
        return shiftRepository.findByTenantIdAndCompanyIdAndCashierIdAndStatus(tenantId, companyId, cashierId, ShiftStatus.ABERTO.name())
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CashShift> findAllByCompanyId(UUID tenantId, UUID companyId, int page, int size) {
        List<CashShiftJpaEntity> entities = shiftRepository.findAllByTenantIdAndCompanyIdOrderByOpenedAtDesc(
                tenantId, companyId, PageRequest.of(Math.max(0, page), Math.max(1, size))
        );
        return entities.stream().map(this::toDomain).toList();
    }

    private CashShift toDomain(CashShiftJpaEntity entity) {
        String cashierName = userRepository.findById(entity.getCashierId())
                .map(UserJpaEntity::getName).orElse("Operador " + entity.getCashierId());

        List<CashMovementJpaEntity> movementEntities = movementRepository.findAllByShiftIdOrderByCreatedAtAsc(entity.getId());
        List<CashMovement> movements = movementEntities.stream().map(m -> new CashMovement(
                m.getId(),
                m.getTenantId(),
                entity.getId(),
                CashMovementType.valueOf(m.getMovementType()),
                m.getAmount(),
                PaymentMethod.valueOf(m.getPaymentMethod()),
                m.getReason(),
                m.getOperatorId(),
                m.getCreatedAt()
        )).toList();

        return new CashShift(
                entity.getId(),
                entity.getTenantId(),
                entity.getCompanyId(),
                entity.getCashierId(),
                cashierName,
                entity.getShiftNumber(),
                ShiftStatus.valueOf(entity.getStatus()),
                entity.getOpeningBalance(),
                entity.getExpectedBalance(),
                entity.getActualBalance(),
                entity.getDivergence(),
                entity.getNotes(),
                movements,
                entity.getOpenedAt(),
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
