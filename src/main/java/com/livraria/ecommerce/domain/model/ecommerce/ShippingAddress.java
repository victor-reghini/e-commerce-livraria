package com.livraria.ecommerce.domain.model.ecommerce;

public record ShippingAddress(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {
    public ShippingAddress {
        if (zipCode != null) {
            zipCode = zipCode.replaceAll("\\D", "");
        }
        if (state != null) {
            state = state.trim().toUpperCase();
        }
    }

    public String formatted() {
        StringBuilder sb = new StringBuilder();
        sb.append(street != null ? street : "").append(", ").append(number != null ? number : "S/N");
        if (complement != null && !complement.isBlank()) {
            sb.append(" - ").append(complement);
        }
        sb.append(", ").append(neighborhood != null ? neighborhood : "");
        sb.append(", ").append(city != null ? city : "").append(" - ").append(state != null ? state : "");
        sb.append(", CEP: ").append(zipCode != null ? zipCode : "");
        return sb.toString();
    }
}

