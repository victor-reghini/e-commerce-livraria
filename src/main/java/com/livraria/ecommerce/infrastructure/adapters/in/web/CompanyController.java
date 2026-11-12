package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.CompanyDto;
import com.livraria.ecommerce.application.dto.CreateCompanyRequest;
import com.livraria.ecommerce.application.dto.UpdateCompanyRequest;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.port.in.CompanyUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Empresas e Filiais", description = "Gestão de Unidades, Filiais, Balcões e Stands de Feiras")
public class CompanyController {

    private final CompanyUseCase companyUseCase;

    public CompanyController(CompanyUseCase companyUseCase) {
        this.companyUseCase = companyUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMPANY_MANAGE')")
    @Operation(summary = "Cadastrar uma nova empresa, filial ou unidade de evento")
    public ResponseEntity<CompanyDto> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        CompanyUseCase.CreateCompanyCommand command = new CompanyUseCase.CreateCompanyCommand(
                tenantId,
                request.code(),
                request.name(),
                request.tradeName(),
                request.cnpj(),
                request.stateRegistration(),
                request.municipalRegistration(),
                request.taxRegime(),
                request.headquarters(),
                request.address()
        );

        Company created = companyUseCase.createCompany(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyDto.fromDomain(created));
    }

    @GetMapping
    @Operation(summary = "Listar todas as filiais e unidades do tenant ativo")
    public ResponseEntity<List<CompanyDto>> listCompanies() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<CompanyDto> list = companyUseCase.listCompaniesByTenant(tenantId).stream()
                .map(CompanyDto::fromDomain)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes de uma filial pelo ID")
    public ResponseEntity<CompanyDto> getCompanyById(@PathVariable UUID id) {
        Company company = companyUseCase.getCompanyById(id);
        return ResponseEntity.ok(CompanyDto.fromDomain(company));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPANY_MANAGE')")
    @Operation(summary = "Atualizar dados cadastrais e endereço da filial")
    public ResponseEntity<CompanyDto> updateCompany(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateCompanyRequest request) {
        CompanyUseCase.UpdateCompanyCommand command = new CompanyUseCase.UpdateCompanyCommand(
                id,
                request.name(),
                request.tradeName(),
                request.cnpj(),
                request.stateRegistration(),
                request.municipalRegistration(),
                request.taxRegime(),
                request.headquarters(),
                request.address()
        );

        Company updated = companyUseCase.updateCompany(command);
        return ResponseEntity.ok(CompanyDto.fromDomain(updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('COMPANY_MANAGE')")
    @Operation(summary = "Ativar ou inativar uma unidade/filial")
    public ResponseEntity<Void> toggleStatus(@PathVariable UUID id, @RequestParam boolean active) {
        companyUseCase.toggleCompanyStatus(id, active);
        return ResponseEntity.noContent().build();
    }
}

