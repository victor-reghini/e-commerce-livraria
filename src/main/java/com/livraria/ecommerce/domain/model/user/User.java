package com.livraria.ecommerce.domain.model.user;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private final UUID tenantId;
    private String name;
    private String email;
    private String passwordHash;
    private UserRole role;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UUID id, UUID tenantId, String name, String email, String passwordHash,
                UserRole role, boolean active, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID é obrigatório");
        this.name = Objects.requireNonNull(name, "Nome do usuário é obrigatório").trim();
        this.email = Objects.requireNonNull(email, "E-mail do usuário é obrigatório").toLowerCase().trim();
        this.passwordHash = Objects.requireNonNull(passwordHash, "Hash da senha é obrigatório");
        this.role = Objects.requireNonNullElse(role, UserRole.CUSTOMER);
        this.active = active;
        this.createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
        this.updatedAt = Objects.requireNonNullElseGet(updatedAt, Instant::now);
    }

    public static User create(UUID tenantId, String name, String email, String passwordHash, UserRole role) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), tenantId, name, email, passwordHash, role, true, now, now);
    }

    public void updateProfile(String name, String email) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (email != null && !email.isBlank()) {
            this.email = email.toLowerCase().trim();
        }
        this.updatedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "Novo hash de senha é obrigatório");
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

