package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.domain.exception.AuthenticationFailedException;
import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.port.in.AuthUseCase;
import com.livraria.ecommerce.domain.port.out.UserRepositoryPort;
import com.livraria.ecommerce.infrastructure.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepositoryPort userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

        String token = jwtTokenProvider.generateToken(user);
        long expiresIn = jwtTokenProvider.getExpirationSeconds();

        return new AuthResult(
                token,
                "Bearer",
                expiresIn,
                user.getId(),
                user.getTenantId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}

