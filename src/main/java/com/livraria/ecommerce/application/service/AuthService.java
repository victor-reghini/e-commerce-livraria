package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.domain.exception.AuthenticationFailedException;
import com.livraria.ecommerce.domain.exception.TenantNotFoundException;
import com.livraria.ecommerce.domain.model.permission.RolePermissions;
import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.port.in.AuthUseCase;
import com.livraria.ecommerce.domain.port.out.TenantRepositoryPort;
import com.livraria.ecommerce.domain.port.out.UserRepositoryPort;
import com.livraria.ecommerce.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String REDIS_REFRESH_PREFIX = "auth:refresh:";
    private static final String REDIS_BLACKLIST_PREFIX = "auth:blacklist:";

    private final UserRepositoryPort userRepository;
    private final TenantRepositoryPort tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepositoryPort userRepository,
                       TenantRepositoryPort tenantRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult authenticate(AuthCommand command) {
        User user = userRepository.findByEmailAndTenantId(command.email(), command.tenantId())
                .orElseThrow(() -> new AuthenticationFailedException("Credenciais inválidas para o tenant informado"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("Usuário inativo no sistema");
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Credenciais inválidas para o tenant informado");
        }

        Set<String> permissions = RolePermissions.getPermissionsForRole(user.getRole());
        String accessToken = jwtTokenProvider.generateAccessToken(user, permissions);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        storeRefreshToken(user.getId(), refreshToken);

        return new AuthResult(
                accessToken,
                refreshToken,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                user.getId(),
                user.getTenantId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                permissions
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationFailedException("Refresh token inválido ou expirado");
        }

        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        UUID tenantId = jwtTokenProvider.getTenantIdFromToken(refreshToken);

        // Verifica se o refresh token ainda é o ativo no Redis
        if (!isRefreshTokenValid(userId, refreshToken)) {
            throw new AuthenticationFailedException("Refresh token revogado ou substituído");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Usuário não encontrado"));

        if (!user.isActive() || !user.getTenantId().equals(tenantId)) {
            throw new AuthenticationFailedException("Usuário inativo ou não pertence ao tenant");
        }

        Set<String> permissions = RolePermissions.getPermissionsForRole(user.getRole());
        String newAccessToken = jwtTokenProvider.generateAccessToken(user, permissions);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        storeRefreshToken(user.getId(), newRefreshToken);

        return new AuthResult(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                user.getId(),
                user.getTenantId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                permissions
        );
    }

    @Override
    public void logout(String token) {
        if (token != null && jwtTokenProvider.validateToken(token)) {
            try {
                UUID userId = jwtTokenProvider.getUserIdFromToken(token);
                redisTemplate.delete(REDIS_REFRESH_PREFIX + userId);
                // Adiciona token à blacklist pelo tempo restante de expiração
                long expiration = jwtTokenProvider.getExpirationSeconds();
                redisTemplate.opsForValue().set(REDIS_BLACKLIST_PREFIX + token, "revoked", Duration.ofSeconds(expiration));
            } catch (Exception e) {
                log.warn("Erro ao registrar logout no Redis: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public TenantPublicInfo resolveTenantForLogin(String domainOrSlug) {
        if (domainOrSlug == null || domainOrSlug.isBlank()) {
            throw new TenantNotFoundException("Identificador de loja não informado");
        }

        String cleaned = domainOrSlug.trim();
        Optional<Tenant> tenantOpt = Optional.empty();

        // 1. Tenta buscar diretamente por ID caso o identificador seja um UUID válido
        try {
            UUID tenantId = UUID.fromString(cleaned);
            tenantOpt = tenantRepository.findById(tenantId);
        } catch (IllegalArgumentException ignored) {
            // Não é UUID, prossegue com busca por slug ou domínio
        }

        // 2. Tenta buscar por slug
        if (tenantOpt.isEmpty()) {
            tenantOpt = tenantRepository.findBySlug(cleaned.toLowerCase());
        }

        // 3. Tenta buscar por domínio
        if (tenantOpt.isEmpty()) {
            tenantOpt = tenantRepository.findByDomain(cleaned.toLowerCase());
        }

        // 4. Fallback para localhost em ambiente de desenvolvimento local quando acessado via IP
        if (tenantOpt.isEmpty() && ("127.0.0.1".equals(cleaned) || "localhost".equalsIgnoreCase(cleaned))) {
            tenantOpt = tenantRepository.findByDomain("localhost");
        }

        Tenant tenant = tenantOpt.orElseThrow(() ->
                new TenantNotFoundException("Livraria/Loja não encontrada com o identificador: " + domainOrSlug));

        return new TenantPublicInfo(
                tenant.getId(),
                tenant.getSlug(),
                tenant.getName(),
                tenant.getTradeName(),
                tenant.getSettings()
        );
    }

    private void storeRefreshToken(UUID userId, String refreshToken) {
        try {
            long duration = jwtTokenProvider.getRefreshExpirationSeconds();
            redisTemplate.opsForValue().set(REDIS_REFRESH_PREFIX + userId, refreshToken, Duration.ofSeconds(duration));
        } catch (Exception e) {
            log.warn("Não foi possível persistir o refresh token no Redis: {}", e.getMessage());
        }
    }

    private boolean isRefreshTokenValid(UUID userId, String refreshToken) {
        try {
            String storedToken = redisTemplate.opsForValue().get(REDIS_REFRESH_PREFIX + userId);
            return storedToken == null || storedToken.equals(refreshToken);
        } catch (Exception e) {
            log.warn("Falha ao checar refresh token no Redis, permitindo fallback: {}", e.getMessage());
            return true;
        }
    }
}
