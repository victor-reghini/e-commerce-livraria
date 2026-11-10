package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.CreateTenantRequest;
import com.livraria.ecommerce.application.dto.TenantDto;
import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.port.in.TenantUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Gestão de Lojistas e Configuração Multi-Tenant")
public class TenantController {

    private final TenantUseCase tenantUseCase;

    public TenantController(TenantUseCase tenantUseCase) {
        this.tenantUseCase = tenantUseCase;
    }

    @PostMapping
    @Operation(summary = "Cadastrar um novo tenant no ecossistema")
    public ResponseEntity<TenantDto> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        TenantUseCase.CreateTenantCommand command = new TenantUseCase.CreateTenantCommand(
                request.slug(),
                request.name(),
                request.tradeName(),
                request.cnpj(),
                request.email(),
                request.phone(),
                request.settings(),
                request.primaryDomain()
        );

        Tenant created = tenantUseCase.createTenant(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantDto.fromDomain(created));
    }

    @GetMapping
    @Operation(summary = "Listar todos os tenants cadastrados")
    public ResponseEntity<List<TenantDto>> listAllTenants() {
        List<TenantDto> list = tenantUseCase.listAllTenants().stream()
                .map(TenantDto::fromDomain)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tenant por UUID")
    public ResponseEntity<TenantDto> getTenantById(@PathVariable UUID id) {
        Tenant tenant = tenantUseCase.getTenantById(id);
        return ResponseEntity.ok(TenantDto.fromDomain(tenant));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Buscar tenant pelo slug")
    public ResponseEntity<TenantDto> getTenantBySlug(@PathVariable String slug) {
        Tenant tenant = tenantUseCase.getTenantBySlug(slug);
        return ResponseEntity.ok(TenantDto.fromDomain(tenant));
    }
}

