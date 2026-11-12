package com.livraria.ecommerce.domain.model.company;

public record Address(
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String state,
        String zipCode
) {}

