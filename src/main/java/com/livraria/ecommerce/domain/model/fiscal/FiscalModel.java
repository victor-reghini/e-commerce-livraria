package com.livraria.ecommerce.domain.model.fiscal;

public enum FiscalModel {
    NFE_55("55", "Nota Fiscal Eletrônica - E-commerce e Operações B2B"),
    NFCE_65("65", "Nota Fiscal de Consumidor Eletrônica - Venda Balcão PDV e Feiras");

    private final String code;
    private final String description;

    FiscalModel(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static FiscalModel fromCode(String code) {
        for (FiscalModel model : values()) {
            if (model.code.equals(code)) {
                return model;
            }
        }
        throw new IllegalArgumentException("Modelo fiscal não suportado: " + code);
    }
}
