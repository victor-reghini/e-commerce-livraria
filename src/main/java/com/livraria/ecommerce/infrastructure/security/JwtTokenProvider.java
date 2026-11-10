package com.livraria.ecommerce.infrastructure.security;

import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.model.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-seconds:86400}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (expirationSeconds * 1000));

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("user_id", user.getId().toString())
                .claim("tenant_id", user.getTenantId().toString())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido ou expirado: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        String userIdStr = getClaims(token).get("user_id", String.class);
        return UUID.fromString(userIdStr);
    }

    public UUID getTenantIdFromToken(String token) {
        String tenantIdStr = getClaims(token).get("tenant_id", String.class);
        return UUID.fromString(tenantIdStr);
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public UserRole getRoleFromToken(String token) {
        String roleStr = getClaims(token).get("role", String.class);
        return UserRole.valueOf(roleStr);
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}

