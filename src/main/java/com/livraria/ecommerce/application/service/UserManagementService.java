package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import com.livraria.ecommerce.domain.port.in.UserManagementUseCase;
import com.livraria.ecommerce.domain.port.out.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserManagementService implements UserManagementUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditUseCase auditUseCase;

    @Autowired
    public UserManagementService(UserRepositoryPort userRepository,
                                 PasswordEncoder passwordEncoder,
                                 @Autowired(required = false) AuditUseCase auditUseCase) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditUseCase = auditUseCase;
    }

    public UserManagementService(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null);
    }

    @Override
    @Transactional
    public User createUser(CreateUserCommand command) {
        if (userRepository.existsByEmailAndTenantId(command.email(), command.tenantId())) {
            throw new BusinessException("Já existe um usuário com este e-mail no tenant informado: " + command.email());
        }

        String hashedPassword = passwordEncoder.encode(command.password());

        User user = User.createWithCompanies(
                command.tenantId(),
                command.name(),
                command.email(),
                hashedPassword,
                command.role(),
                command.companyIds()
        );

        User savedUser = userRepository.save(user);

        if (command.companyIds() != null && !command.companyIds().isEmpty()) {
            userRepository.saveUserCompanies(savedUser.getId(), savedUser.getTenantId(), command.companyIds());
        }

        if (auditUseCase != null) {
            auditUseCase.recordEvent("USER_CREATED", "USER", savedUser.getId().toString(), Map.of(
                    "email", savedUser.getEmail(),
                    "role", savedUser.getRole().name(),
                    "name", savedUser.getName()
            ));
        }

        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listUsersByTenant(UUID tenantId) {
        return userRepository.findAllByTenantId(tenantId);
    }

    @Override
    @Transactional
    public User updateUser(UpdateUserCommand command) {
        User user = getUserById(command.id());
        user.updateProfile(command.name(), command.email(), command.role());
        User saved = userRepository.save(user);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("USER_UPDATED", "USER", saved.getId().toString(), Map.of(
                    "email", saved.getEmail(),
                    "role", saved.getRole().name()
            ));
        }
        return saved;
    }

    @Override
    @Transactional
    public void toggleUserStatus(UUID id, boolean active) {
        User user = getUserById(id);
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        userRepository.save(user);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("USER_STATUS_TOGGLED", "USER", id.toString(), Map.of(
                    "active", active,
                    "email", user.getEmail()
            ));
        }
    }

    @Override
    @Transactional
    public void changePassword(UUID id, String oldPassword, String newPassword) {
        User user = getUserById(id);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("A senha atual informada está incorreta.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("USER_PASSWORD_CHANGED", "USER", id.toString(), Map.of(
                    "email", user.getEmail()
            ));
        }
    }

    @Override
    @Transactional
    public void assignCompanies(UUID id, List<UUID> companyIds) {
        User user = getUserById(id);
        user.assignCompanies(companyIds);
        userRepository.saveUserCompanies(user.getId(), user.getTenantId(), companyIds);
        if (auditUseCase != null) {
            auditUseCase.recordEvent("USER_PERMISSIONS_CHANGED", "USER", id.toString(), Map.of(
                    "email", user.getEmail(),
                    "assignedCompaniesCount", companyIds.size()
            ));
        }
    }
}

