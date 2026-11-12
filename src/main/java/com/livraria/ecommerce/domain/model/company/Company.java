package com.livraria.ecommerce.domain.model.company;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Company {

    private final UUID id;
    private final UUID tenantId;
    private final String code;
    private String name;
    private String tradeName;
    private String cnpj;
    private String stateRegistration;
    private String municipalRegistration;
    private TaxRegime taxRegime;
    private boolean headquarters;
    private boolean active;
    private Address address;
    private final Instant createdAt;
    private Instant updatedAt;

    public Company(UUID id, UUID tenantId, String code, String name, String tradeName,
                   String cnpj, String stateRegistration, String municipalRegistration,
                   TaxRegime taxRegime, boolean headquarters, boolean active,
                   Address address, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        this.tenantId = Objects.requireNonNull(tenantId, "Tenant ID é obrigatório");
        this.code = Objects.requireNonNull(code, "Código da empresa/filial é obrigatório").toUpperCase().trim();
        this.name = Objects.requireNonNull(name, "Razão Social/Nome é obrigatório").trim();
        this.tradeName = tradeName != null ? tradeName.trim() : null;
        this.cnpj = cnpj != null ? cnpj.replaceAll("\\D", "") : null;
        this.stateRegistration = stateRegistration;
        this.municipalRegistration = municipalRegistration;
        this.taxRegime = Objects.requireNonNullElse(taxRegime, TaxRegime.SIMPLES_NACIONAL);
        this.headquarters = headquarters;
        this.active = active;
        this.address = address;
        this.createdAt = Objects.requireNonNullElseGet(createdAt, Instant::now);
        this.updatedAt = Objects.requireNonNullElseGet(updatedAt, Instant::now);
    }

    public static Company create(UUID tenantId, String code, String name, String tradeName,
                                 String cnpj, String stateRegistration, String municipalRegistration,
                                 TaxRegime taxRegime, boolean headquarters, Address address) {
        Instant now = Instant.now();
        return new Company(UUID.randomUUID(), tenantId, code, name, tradeName, cnpj,
                stateRegistration, municipalRegistration, taxRegime, headquarters, true, address, now, now);
    }

    public void update(String name, String tradeName, String cnpj, String stateRegistration,
                       String municipalRegistration, TaxRegime taxRegime, boolean headquarters, Address address) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (tradeName != null) {
            this.tradeName = tradeName.trim();
        }
        if (cnpj != null) {
            this.cnpj = cnpj.replaceAll("\\D", "");
        }
        this.stateRegistration = stateRegistration;
        this.municipalRegistration = municipalRegistration;
        if (taxRegime != null) {
            this.taxRegime = taxRegime;
        }
        this.headquarters = headquarters;
        if (address != null) {
            this.address = address;
        }
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

    public String getCode() {
        return code;
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

    public String getStateRegistration() {
        return stateRegistration;
    }

    public String getMunicipalRegistration() {
        return municipalRegistration;
    }

    public TaxRegime getTaxRegime() {
        return taxRegime;
    }

    public boolean isHeadquarters() {
        return headquarters;
    }

    public boolean isActive() {
        return active;
    }

    public Address getAddress() {
        return address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

