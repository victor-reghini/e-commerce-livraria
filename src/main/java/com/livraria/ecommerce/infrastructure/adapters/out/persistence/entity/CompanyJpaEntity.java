package com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity;

import com.livraria.ecommerce.domain.model.company.TaxRegime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "code", nullable = false, length = 32)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "cnpj", length = 18)
    private String cnpj;

    @Column(name = "state_registration", length = 32)
    private String stateRegistration;

    @Column(name = "municipal_registration", length = 32)
    private String municipalRegistration;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 32)
    private TaxRegime taxRegime;

    @Column(name = "is_headquarters", nullable = false)
    private boolean headquarters;

    @Column(name = "active", nullable = false)
    private boolean active;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "address_json", columnDefinition = "jsonb")
    private String addressJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
