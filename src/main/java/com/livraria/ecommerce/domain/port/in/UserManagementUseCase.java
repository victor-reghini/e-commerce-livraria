package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.model.user.UserRole;

import java.util.List;
import java.util.UUID;

public interface UserManagementUseCase {

    User createUser(CreateUserCommand command);

    User getUserById(UUID id);

    List<User> listUsersByTenant(UUID tenantId);

    User updateUser(UpdateUserCommand command);

    void toggleUserStatus(UUID id, boolean active);

    void changePassword(UUID id, String oldPassword, String newPassword);

    void assignCompanies(UUID id, List<UUID> companyIds);

    record CreateUserCommand(
            UUID tenantId,
            String name,
            String email,
            String password,
            UserRole role,
            List<UUID> companyIds
    ) {}

    record UpdateUserCommand(
            UUID id,
            String name,
            String email,
            UserRole role
    ) {}
}

