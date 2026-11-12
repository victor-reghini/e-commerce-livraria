package com.livraria.ecommerce.domain.model.fiscal;

import java.time.Instant;

public record SefazAuthResult(
        boolean authorized,
        int statusCode,
        String statusMessage,
        String protocolNumber,
        String accessKey,
        String digestValue,
        Instant authorizedAt,
        boolean contingency,
        String rawXml
) {
    public static SefazAuthResult success(String protocolNumber, String accessKey, String digestValue, String rawXml) {
        return new SefazAuthResult(true, 100, "Autorizado o uso da NF-e", protocolNumber, accessKey, digestValue, Instant.now(), false, rawXml);
    }

    public static SefazAuthResult contingency(String accessKey, String reason, String rawXml) {
        return new SefazAuthResult(false, 999, "Emissão autorizada em Contingência Offline: " + reason, null, accessKey, null, Instant.now(), true, rawXml);
    }

    public static SefazAuthResult rejection(int statusCode, String reason) {
        return new SefazAuthResult(false, statusCode, reason, null, null, null, null, false, null);
    }
}
