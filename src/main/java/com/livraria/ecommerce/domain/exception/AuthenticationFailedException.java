package com.livraria.ecommerce.domain.exception;

public class AuthenticationFailedException extends BusinessException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}

