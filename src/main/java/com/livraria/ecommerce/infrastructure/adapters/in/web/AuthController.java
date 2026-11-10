package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.AuthRequest;
import com.livraria.ecommerce.application.dto.AuthResponse;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.port.in.AuthUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação multi-tenant e geração de JWT")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário gerando JWT com tenant_id, role e claims")
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
}

