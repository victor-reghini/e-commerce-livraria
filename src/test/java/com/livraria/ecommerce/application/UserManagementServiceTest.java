package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.service.UserManagementService;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.model.user.UserRole;
import com.livraria.ecommerce.domain.port.in.UserManagementUseCase;
import com.livraria.ecommerce.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userService;

    private UUID tenantId;
    private UserManagementUseCase.CreateUserCommand createCommand;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        createCommand = new UserManagementUseCase.CreateUserCommand(
                tenantId,
                "Lucas Atendente",
                "lucas@livraria.com",
                "Senha@123",
                UserRole.ATTENDANT,
                List.of(UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Deve criar usuário com senha criptografada e empresas vinculadas")
    void shouldCreateUserSuccessfully() {
        when(userRepository.existsByEmailAndTenantId("lucas@livraria.com", tenantId)).thenReturn(false);
        when(passwordEncoder.encode("Senha@123")).thenReturn("$2a$10$hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.createUser(createCommand);

        assertNotNull(user);
        assertEquals("Lucas Atendente", user.getName());
        assertEquals("lucas@livraria.com", user.getEmail());
        assertEquals("$2a$10$hashedPass", user.getPasswordHash());
        assertEquals(UserRole.ATTENDANT, user.getRole());
        verify(userRepository).save(any(User.class));
        verify(userRepository).saveUserCompanies(eq(user.getId()), eq(tenantId), anyList());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando e-mail já existir no tenant")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailAndTenantId("lucas@livraria.com", tenantId)).thenReturn(true);

        assertThrows(BusinessException.class, () -> userService.createUser(createCommand));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve buscar usuário por ID")
    void shouldGetUserById() {
        UUID userId = UUID.randomUUID();
        User user = User.create(tenantId, "Admin", "admin@livraria.com", "hash", UserRole.TENANT_ADMIN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User found = userService.getUserById(userId);
        assertEquals("Admin", found.getName());
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException quando usuário não for encontrado")
    void shouldThrowExceptionWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(userId));
    }

    @Test
    @DisplayName("Deve listar todos os usuários de um tenant")
    void shouldListUsersByTenant() {
        User u1 = User.create(tenantId, "U1", "u1@livraria.com", "hash", UserRole.STORE_MANAGER);
        User u2 = User.create(tenantId, "U2", "u2@livraria.com", "hash", UserRole.ATTENDANT);

        when(userRepository.findAllByTenantId(tenantId)).thenReturn(List.of(u1, u2));

        List<User> list = userService.listUsersByTenant(tenantId);
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Deve alterar senha quando senha atual estiver correta")
    void shouldChangePasswordSuccessfully() {
        UUID userId = UUID.randomUUID();
        User user = User.create(tenantId, "Lucas", "lucas@livraria.com", "$2a$10$oldHash", UserRole.ATTENDANT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword", "$2a$10$oldHash")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword")).thenReturn("$2a$10$newHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.changePassword(userId, "OldPassword", "NewPassword");

        assertEquals("$2a$10$newHash", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Deve falhar ao alterar senha se senha atual for inválida")
    void shouldFailChangePasswordWithInvalidOldPassword() {
        UUID userId = UUID.randomUUID();
        User user = User.create(tenantId, "Lucas", "lucas@livraria.com", "$2a$10$oldHash", UserRole.ATTENDANT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$oldHash")).thenReturn(false);

        assertThrows(BusinessException.class, () ->
                userService.changePassword(userId, "WrongPassword", "NewPassword")
        );
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve alternar ativação do usuário")
    void shouldToggleUserStatus() {
        UUID userId = UUID.randomUUID();
        User user = User.create(tenantId, "Lucas", "lucas@livraria.com", "hash", UserRole.ATTENDANT);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.toggleUserStatus(userId, false);
        assertFalse(user.isActive());

        userService.toggleUserStatus(userId, true);
        assertTrue(user.isActive());
    }
}

