package com.livraria.ecommerce.domain.model.tenant;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Tenant {

    private final UUID id;
    private final String slug;
    private String name;
    private String tradeName;
    private String cnpj;
    private String email;
    private String phone;
    private TenantStatus status;
    private Map<String, Object> settings;
    private final Instant createdAt;
    private Instant updatedAt;

    public Tenant(UUID id, String slug, String name, String tradeName, String cnpj,
                  String email, String phone, TenantStatus status,
                  Map<String, Object> settings, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        this.slug = Objects.requireNonNull(slug, "Slug do tenant é obrigatório").toLowerCase().trim();
        this.name = Objects.requireNonNull(name, "Nome do tenant é obrigatório").trim();
        this.tradeName = tradeName;
        this.cnpj = cnpj;
        this.email = Objects.requireNonNull(email, "E-mail do tenant é obrigatório").trim();
        this.phone = phone;
        this.status = Objects.requireNonNullElse(status, TenantStatus.ACTIVE);
        this.settings = settings != null ? Map.copyOf(settings) : Collections.emptyMap();
        this.createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
        this.updatedAt = Objects.requireNonNullElseGet(updatedAt, Instant::now);
    }

    public static Tenant create(String slug, String name, String tradeName, String cnpj,
                                String email, String phone, Map<String, Object> settings) {
        Instant now = Instant.now();
        return new Tenant(UUID.randomUUID(), slug, name, tradeName, cnpj, email, phone,
                TenantStatus.ACTIVE, settings, now, now);
    }

    public void update(String name, String tradeName, String cnpj, String email,
                       String phone, Map<String, Object> settings) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.tradeName = tradeName;
        this.cnpj = cnpj;
        if (email != null && !email.isBlank()) {
            this.email = email.trim();
        }
        this.phone = phone;
        if (settings != null) {
            this.settings = Map.copyOf(settings);
        }
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = TenantStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        this.status = TenantStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == TenantStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getTradeName() {
        return tradeName;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

