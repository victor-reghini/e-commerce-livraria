package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.application.dto.stock.EventStockDto;
import com.livraria.ecommerce.domain.model.stock.BlindAudit;
import com.livraria.ecommerce.domain.model.stock.EventDashboard;
import com.livraria.ecommerce.domain.model.stock.StockMovement;
import com.livraria.ecommerce.domain.model.stock.StockTransfer;

import java.util.List;
import java.util.UUID;

public interface EventStockUseCase {

    StockTransfer createTransfer(EventStockDto.CreateTransferRequest request, UUID createdBy);

    StockTransfer dispatchTransfer(UUID transferId, UUID dispatchedBy);

    StockTransfer receiveTransfer(UUID transferId, EventStockDto.ReceiveTransferRequest request, UUID receivedBy);

    StockTransfer getTransferById(UUID transferId);

    List<StockTransfer> listTransfersByCompany(UUID companyId);

    List<StockTransfer> listAllTransfers();

    EventStockDto.RomaneioDocumentResponse generateRomaneio(UUID transferId);

    BlindAudit createBlindAudit(EventStockDto.CreateAuditRequest request, UUID auditorId);

    BlindAudit scanBarcode(UUID auditId, String barcode, int quantity);

    BlindAudit finalizeAudit(UUID auditId);

    BlindAudit reconcileAudit(UUID auditId, UUID reconciledBy);

    BlindAudit getBlindAuditById(UUID auditId);

    List<BlindAudit> listBlindAuditsByCompany(UUID companyId);

    List<StockMovement> listMovementsByCompany(UUID companyId, int page, int size);

    EventDashboard getEventDashboard(UUID companyId);
}
