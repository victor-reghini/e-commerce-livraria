package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.ecommerce.PaymentTransactionResult;

import java.math.BigDecimal;
import java.util.UUID;

public interface PagarmeGatewayPort {
    PaymentTransactionResult processPix(UUID tenantId, String orderNumber, BigDecimal amount,
                                       String customerName, String customerEmail, String customerDocument);

    PaymentTransactionResult processCreditCard(UUID tenantId, String orderNumber, BigDecimal amount,
                                               int installments, String cardToken, String customerName,
                                               String customerEmail, String customerDocument);

    PaymentTransactionResult processBoleto(UUID tenantId, String orderNumber, BigDecimal amount,
                                          int dueDays, String customerName, String customerEmail,
                                          String customerDocument);
}

