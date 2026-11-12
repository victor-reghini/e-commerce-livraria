package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.fiscal.FiscalCancellationResult;
import com.livraria.ecommerce.domain.model.fiscal.SefazAuthResult;
import com.livraria.ecommerce.domain.model.fiscal.SefazStatusResult;

public interface SefazGatewayPort {

    SefazStatusResult checkServiceStatus(String uf, String environment);

    SefazAuthResult authorizeInvoice(String xmlPayload, String accessKey, String model, String uf);

    FiscalCancellationResult cancelInvoice(String accessKey, String protocolNumber, String reason, String uf);
}

