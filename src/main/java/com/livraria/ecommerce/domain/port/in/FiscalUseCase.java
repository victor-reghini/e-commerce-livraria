package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import com.livraria.ecommerce.domain.model.fiscal.SefazStatusResult;

import java.util.List;
import java.util.UUID;

public interface FiscalUseCase {

    FiscalInvoice emitNfeForOrder(UUID orderId, int series);

    FiscalInvoice emitNfceForSale(UUID saleId, int series);

    FiscalInvoice findById(UUID id);

    FiscalInvoice findByAccessKey(String accessKey);

    String getInvoiceXml(UUID id);

    String generateDanfeMirror(UUID id);

    FiscalInvoice cancelInvoice(UUID id, String reason);

    SefazStatusResult consultSefazStatus(String uf);

    List<FiscalInvoice> listInvoices(int page, int size);
}

