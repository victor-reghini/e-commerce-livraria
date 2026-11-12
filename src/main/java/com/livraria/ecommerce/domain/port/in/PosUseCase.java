package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.application.dto.pos.PosDto;
import com.livraria.ecommerce.domain.model.pos.CashShift;
import com.livraria.ecommerce.domain.model.pos.PosSale;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosUseCase {

    CashShift openShift(UUID companyId, BigDecimal openingBalance, UUID cashierId);

    CashShift supplyCash(UUID shiftId, BigDecimal amount, String reason, UUID operatorId);

    CashShift bleedCash(UUID shiftId, BigDecimal amount, String reason, UUID operatorId);

    CashShift closeShift(UUID shiftId, BigDecimal countedCash, String notes, UUID operatorId);

    CashShift getShiftById(UUID shiftId);

    Optional<CashShift> getCurrentOpenShift(UUID companyId, UUID cashierId);

    List<CashShift> listShifts(UUID companyId, int page, int size);

    PosSale createSale(PosDto.CreateSaleRequest request, UUID cashierId);

    PosSale cancelSale(UUID saleId, String reason, UUID operatorId);

    PosSale getSaleById(UUID saleId);

    List<PosSale> listSalesByShift(UUID shiftId);

    List<PosSale> listSalesByCompany(UUID companyId, int page, int size);

    PosDto.OfflineCatalogSyncResponse syncCatalog(UUID companyId, Instant updatedSince);

    PosDto.OfflineBatchSyncResult syncOfflineSales(List<PosDto.CreateSaleRequest> sales, UUID cashierId);

    PosDto.ThermalReceiptResponse getThermalReceipt(UUID saleId);
}
