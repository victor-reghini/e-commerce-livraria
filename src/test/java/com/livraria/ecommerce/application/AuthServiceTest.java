package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.service.AuthService;
import com.livraria.ecommerce.domain.exception.AuthenticationFailedException;
import com.livraria.ecommerce.domain.exception.TenantNotFoundException;
import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.model.user.UserRole;
import com.livraria.ecommerce.domain.port.in.AuthUseCase;
import com.livraria.ecommerce.domain.port.out.TenantRepositoryPort;
import com.livraria.ecommerce.domain.port.out.UserRepositoryPort;
import com.livraria.ecommerce.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private TenantRepositoryPort tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private UUID tenantId;
    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        activeUser = new User(userId, tenantId, "Admin", "admin@livraria.com", "$2a$10$hashedPass",
                UserRole.TENANT_ADMIN, true, null, null);
    }

    @Test
    @DisplayName("Deve autenticar com sucesso gerando tokens de acesso e refresh")
    void shouldAuthenticateSuccessfully() {
        when(userRepository.findByEmailAndTenantId("admin@livraria.com", tenantId)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Senha@123", "$2a$10$hashedPass")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(eq(activeUser), anySet())).thenReturn("jwt-access-token");
        when(jwtTokenProvider.generateRefreshToken(activeUser)).thenReturn("jwt-refresh-token");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(86400L);
        when(jwtTokenProvider.getRefreshExpirationSeconds()).thenReturn(604800L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        AuthUseCase.AuthCommand command = new AuthUseCase.AuthCommand(tenantId, "admin@livraria.com", "Senha@123");
        AuthUseCase.AuthResult result = authService.authenticate(command);

        assertNotNull(result);
        assertEquals("jwt-access-token", result.accessToken());
        assertEquals("jwt-refresh-token", result.refreshToken());
        assertEquals(userId, result.userId());
        assertEquals(tenantId, result.tenantId());
        assertEquals(UserRole.TENANT_ADMIN, result.role());
        assertTrue(result.permissions().contains("CATALOG_WRITE"));
    }

    @Test
    @DisplayName("Deve falhar autenticação quando senha for inválida")
    void shouldFailWhenPasswordInvalid() {
        when(userRepository.findByEmailAndTenantId("admin@livraria.com", tenantId)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SenhaErrada", "$2a$10$hashedPass")).thenReturn(false);

        AuthUseCase.AuthCommand command = new AuthUseCase.AuthCommand(tenantId, "admin@livraria.com", "SenhaErrada");
        assertThrows(AuthenticationFailedException.class, () -> authService.authenticate(command));
    }

    @Test
    @DisplayName("Deve falhar autenticação quando usuário estiver inativo")
    void shouldFailWhenUserInactive() {
        activeUser.deactivate();
        when(userRepository.findByEmailAndTenantId("admin@livraria.com", tenantId)).thenReturn(Optional.of(activeUser));

        AuthUseCase.AuthCommand command = new AuthUseCase.AuthCommand(tenantId, "admin@livraria.com", "Senha@123");
        assertThrows(AuthenticationFailedException.class, () -> authService.authenticate(command));
    }

    @Test
    @DisplayName("Deve resolver informações públicas da loja para login pelo slug")
    void shouldResolveTenantInfoBySlug() {
        Tenant tenant = new Tenant(tenantId, "matriz", "Livraria Matriz", "Matriz", "123", "contato@livraria.com",
                null, null, Map.of("primaryColor", "#ff0000"), null, null);

        when(tenantRepository.findBySlug("matriz")).thenReturn(Optional.of(tenant));

        AuthUseCase.TenantPublicInfo info = authService.resolveTenantForLogin("matriz");

        assertNotNull(info);
        assertEquals("matriz", info.slug());
        assertEquals("Livraria Matriz", info.name());
        assertEquals("#ff0000", info.visualSettings().get("primaryColor"));
    }

    @Test
    @DisplayName("Deve lançar TenantNotFoundException se loja não for encontrada")
    void shouldThrowExceptionWhenTenantNotFoundForLogin() {
        when(tenantRepository.findBySlug("desconhecido")).thenReturn(Optional.empty());
        when(tenantRepository.findByDomain("desconhecido")).thenReturn(Optional.empty());

        assertThrows(TenantNotFoundException.class, () -> authService.resolveTenantForLogin("desconhecido"));
    }
}

