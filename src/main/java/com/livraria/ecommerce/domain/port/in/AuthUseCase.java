package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.domain.model.user.UserRole;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface AuthUseCase {

    AuthResult authenticate(AuthCommand command);

    AuthResult refreshToken(String refreshToken);

    void logout(String token);

    TenantPublicInfo resolveTenantForLogin(String domainOrSlug);

    record AuthCommand(
            UUID tenantId,
            String email,
            String password
    ) {}

    record AuthResult(
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
    ) {}

    record TenantPublicInfo(
            UUID tenantId,
            String slug,
            String name,
            String tradeName,
            Map<String, Object> visualSettings
    ) {}
}
