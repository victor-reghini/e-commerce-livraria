package com.livraria.ecommerce.domain.model.fiscal;

public enum FiscalStatus {
    EMITIDA("Documento fiscal gerado localmente, aguardando transmissão"),
    AUTORIZADA("Uso autorizado pela Secretaria da Fazenda (SEFAZ)"),
    CANCELADA("Cancelamento homologado perante a SEFAZ com registro de evento"),
    CONTINGENCIA("Emitida em contingência offline devido a indisponibilidade SEFAZ"),
    REJEITADA("Transmissão rejeitada pela SEFAZ por erro de validação de regras");

    private final String description;

    FiscalStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
