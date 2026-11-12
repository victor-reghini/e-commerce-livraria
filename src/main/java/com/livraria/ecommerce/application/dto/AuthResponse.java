package com.livraria.ecommerce.application.dto;

import com.livraria.ecommerce.domain.model.user.UserRole;
import com.livraria.ecommerce.domain.port.in.AuthUseCase;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        UUID tenantId,
        String name,
        String email,
        UserRole role,
        Set<String> permissions
) {
    public static AuthResponse fromResult(AuthUseCase.AuthResult result) {
        return new AuthResponse(
                result.accessToken(),
                result.refreshToken(),
                result.tokenType(),
                result.expiresInSeconds(),
                result.userId(),
                result.tenantId(),
                result.name(),
                result.email(),
                result.role(),
                result.permissions()
        );
    }
}
