package com.livraria.ecommerce.domain.model.ecommerce;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentTransactionResult(
        String gatewayTransactionId,
        PaymentStatus status,
        EcommercePaymentMethod method,
        BigDecimal amount,
        int installments,
        String pixQrCode,
        String pixCopiaCola,
        String boletoBarcode,
        String boletoUrl,
        LocalDate boletoDueDate,
        String cardBrand,
        String cardLastFour,
        String errorMessage
) {
    public static PaymentTransactionResult pix(String txId, BigDecimal amount, String qrCode, String copiaCola) {
        return new PaymentTransactionResult(txId, PaymentStatus.PENDENTE, EcommercePaymentMethod.PIX,
                amount, 1, qrCode, copiaCola, null, null, null, null, null, null);
    }

    public static PaymentTransactionResult creditCard(String txId, PaymentStatus status, BigDecimal amount,
                                                      int installments, String brand, String lastFour) {
        return new PaymentTransactionResult(txId, status, EcommercePaymentMethod.CREDIT_CARD,
                amount, installments, null, null, null, null, null, brand, lastFour, null);
    }

    public static PaymentTransactionResult boleto(String txId, BigDecimal amount, String barcode, String url, LocalDate dueDate) {
        return new PaymentTransactionResult(txId, PaymentStatus.PENDENTE, EcommercePaymentMethod.BOLETO,
                amount, 1, null, null, barcode, url, dueDate, null, null, null);
    }

    public static PaymentTransactionResult failed(EcommercePaymentMethod method, BigDecimal amount, String error) {
        return new PaymentTransactionResult(null, PaymentStatus.RECUSADO, method,
                amount, 1, null, null, null, null, null, null, null, error);
    }
}

