package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.ChangePasswordRequest;
import com.livraria.ecommerce.application.dto.CreateUserRequest;
import com.livraria.ecommerce.application.dto.UpdateUserRequest;
import com.livraria.ecommerce.application.dto.UserDto;
import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.port.in.UserManagementUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import com.livraria.ecommerce.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/v1/users")
@Tag(name = "Usuários", description = "Gestão de Usuários, Colaboradores e Permissões do Tenant")
public class UserController {

    private final UserManagementUseCase userManagementUseCase;

    public UserController(UserManagementUseCase userManagementUseCase) {
        this.userManagementUseCase = userManagementUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Operation(summary = "Cadastrar um novo usuário no tenant ativo")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        UUID tenantId = TenantContext.getRequiredTenantId();

        UserManagementUseCase.CreateUserCommand command = new UserManagementUseCase.CreateUserCommand(
                tenantId,
                request.name(),
                request.email(),
                request.password(),
                request.role(),
                request.companyIds()
        );

        User created = userManagementUseCase.createUser(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.fromDomain(created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Operation(summary = "Listar todos os usuários do tenant ativo")
    public ResponseEntity<List<UserDto>> listUsers() {
        UUID tenantId = TenantContext.getRequiredTenantId();
        List<UserDto> list = userManagementUseCase.listUsersByTenant(tenantId).stream()
                .map(UserDto::fromDomain)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/me")
    @Operation(summary = "Obter dados do perfil do usuário autenticado atual")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userManagementUseCase.getUserById(principal.getId());
        return ResponseEntity.ok(UserDto.fromDomain(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE') or #id == authentication.principal.id")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        User user = userManagementUseCase.getUserById(id);
        return ResponseEntity.ok(UserDto.fromDomain(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Operation(summary = "Atualizar perfil e papel do usuário")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id,
                                             @Valid @RequestBody UpdateUserRequest request) {
        UserManagementUseCase.UpdateUserCommand command = new UserManagementUseCase.UpdateUserCommand(
                id,
                request.name(),
                request.email(),
                request.role()
        );

        User updated = userManagementUseCase.updateUser(command);
        return ResponseEntity.ok(UserDto.fromDomain(updated));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Operation(summary = "Ativar ou inativar usuário")
    public ResponseEntity<Void> toggleStatus(@PathVariable UUID id, @RequestParam boolean active) {
        userManagementUseCase.toggleUserStatus(id, active);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasAuthority('USER_MANAGE') or #id == authentication.principal.id")
    @Operation(summary = "Alterar senha do usuário")
    public ResponseEntity<Void> changePassword(@PathVariable UUID id,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userManagementUseCase.changePassword(id, request.oldPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/companies")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    @Operation(summary = "Vincular filiais e unidades de eventos que o usuário pode operar")
    public ResponseEntity<Void> assignCompanies(@PathVariable UUID id, @RequestBody List<UUID> companyIds) {
        userManagementUseCase.assignCompanies(id, companyIds);
        return ResponseEntity.noContent().build();
    }
}

