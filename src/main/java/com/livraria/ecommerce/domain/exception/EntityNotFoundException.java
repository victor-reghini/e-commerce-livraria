package com.livraria.ecommerce.domain.exception;

public class EntityNotFoundException extends BusinessException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entity, Object id) {
        super(entity + " não encontrado(a) com identificador: " + id);
    }
}

