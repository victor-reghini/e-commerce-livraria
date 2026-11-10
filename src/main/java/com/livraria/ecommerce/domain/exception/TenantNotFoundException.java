package com.livraria.ecommerce.domain.exception;

import java.util.UUID;

public class TenantNotFoundException extends EntityNotFoundException {
    public TenantNotFoundException(UUID id) {
        super("Tenant não encontrado com ID: " + id);
    }

    public TenantNotFoundException(String identifier) {
        super("Tenant não encontrado com identificador: " + identifier);
    }
}

