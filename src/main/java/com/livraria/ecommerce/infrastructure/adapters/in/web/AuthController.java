package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.AuthRequest;
import com.livraria.ecommerce.application.dto.AuthResponse;
import com.livraria.ecommerce.application.dto.RefreshTokenRequest;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.port.in.AuthUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação multi-tenant, refresh token e resolução de loja")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário gerando Access Token e Refresh Token com RBAC")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        UUID effectiveTenantId = request.tenantId() != null
                ? request.tenantId()
                : TenantContext.getTenantId().orElseThrow(() ->
                new BusinessException("Identificador do Tenant é obrigatório (envie via body ou header X-Tenant-ID)")
        );

        AuthUseCase.AuthCommand command = new AuthUseCase.AuthCommand(
                effectiveTenantId,
                request.email(),
                request.password()
        );

        AuthUseCase.AuthResult result = authUseCase.authenticate(command);
        return ResponseEntity.ok(AuthResponse.fromResult(result));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar Access Token utilizando Refresh Token válido")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthUseCase.AuthResult result = authUseCase.refreshToken(request.refreshToken());
        return ResponseEntity.ok(AuthResponse.fromResult(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão e revogar tokens ativos no Redis")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            authUseCase.logout(authHeader.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenant-info")
    @Operation(summary = "Resolver informações visuais e dados da loja para personalização da tela de login no Frontend")
    public ResponseEntity<AuthUseCase.TenantPublicInfo> getTenantInfo(
            @RequestParam(required = false) String identifier,
            HttpServletRequest request
    ) {
        String effectiveIdentifier = identifier;

        if (!StringUtils.hasText(effectiveIdentifier)) {
            // Tenta obter do header X-Tenant-ID
            String headerTenant = request.getHeader("X-Tenant-ID");
            if (StringUtils.hasText(headerTenant)) {
                effectiveIdentifier = headerTenant;
            } else {
                // Tenta obter pelo nome do Host da requisição
                effectiveIdentifier = request.getServerName();
            }
        }

        AuthUseCase.TenantPublicInfo info = authUseCase.resolveTenantForLogin(effectiveIdentifier);
        return ResponseEntity.ok(info);
    }
}
