package com.livraria.ecommerce.domain.model.user;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private List<UUID> companyIds;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(UUID id, UUID tenantId, String name, String email, String passwordHash,
                UserRole role, boolean active, List<UUID> companyIds, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID é obrigatório");
        this.name = Objects.requireNonNull(name, "Nome do usuário é obrigatório").trim();
        this.email = Objects.requireNonNull(email, "E-mail do usuário é obrigatório").toLowerCase().trim();
        this.passwordHash = Objects.requireNonNull(passwordHash, "Hash da senha é obrigatório");
        this.role = Objects.requireNonNullElse(role, UserRole.CUSTOMER);
        this.active = active;
        this.companyIds = companyIds != null ? new ArrayList<>(companyIds) : new ArrayList<>();
        this.createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
        this.updatedAt = Objects.requireNonNullElseGet(updatedAt, Instant::now);
    }

    public User(UUID id, UUID tenantId, String name, String email, String passwordHash,
                UserRole role, boolean active, Instant createdAt, Instant updatedAt) {
        this(id, tenantId, name, email, passwordHash, role, active, Collections.emptyList(), createdAt, updatedAt);
    }

    public static User create(UUID tenantId, String name, String email, String passwordHash, UserRole role) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), tenantId, name, email, passwordHash, role, true, new ArrayList<>(), now, now);
    }

    public static User createWithCompanies(UUID tenantId, String name, String email, String passwordHash,
                                          UserRole role, List<UUID> companyIds) {
        Instant now = Instant.now();
        return new User(UUID.randomUUID(), tenantId, name, email, passwordHash, role, true, companyIds, now, now);
    }

    public void updateProfile(String name, String email, UserRole role) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (email != null && !email.isBlank()) {
            this.email = email.toLowerCase().trim();
        }
        if (role != null) {
            this.role = role;
        }
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String name, String email) {
        updateProfile(name, email, null);
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "Novo hash de senha é obrigatório");
        this.updatedAt = Instant.now();
    }

    public void assignCompanies(List<UUID> newCompanyIds) {
        if (newCompanyIds != null) {
            this.companyIds = new ArrayList<>(newCompanyIds);
            this.updatedAt = Instant.now();
        }
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

    public List<UUID> getCompanyIds() {
        return Collections.unmodifiableList(companyIds);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
