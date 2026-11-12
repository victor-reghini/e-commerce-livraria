package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.port.in.UserManagementUseCase;
import com.livraria.ecommerce.domain.port.out.UserRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserManagementService implements UserManagementUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        return userRepository.save(user);
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
    }

    @Override
    @Transactional
    public void assignCompanies(UUID id, List<UUID> companyIds) {
        User user = getUserById(id);
        user.assignCompanies(companyIds);
        userRepository.saveUserCompanies(user.getId(), user.getTenantId(), companyIds);
    }
}

