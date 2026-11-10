package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.domain.model.user.UserRole;

import java.util.UUID;

public interface AuthUseCase {

    AuthResult authenticate(AuthCommand command);

    record AuthCommand(
            UUID tenantId,
            String email,
            String password
    ) {}

    record AuthResult(
            String token,
            String tokenType,
            long expiresInSeconds,
            UUID userId,
            UUID tenantId,
            String name,
            String email,
            UserRole role
    ) {}
}

