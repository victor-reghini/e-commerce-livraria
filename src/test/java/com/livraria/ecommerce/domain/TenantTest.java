package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.model.tenant.TenantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TenantTest {

    @Test
    @DisplayName("Deve criar um tenant válido com status ACTIVE por padrão")
    void shouldCreateValidTenant() {
        Tenant tenant = Tenant.create(
                "livraria-cultura",
                "Livraria Cultura Ltda",
                "Livraria Cultura",
                "12345678000199",
                "contato@cultura.com.br",
                "11988887777",
                Map.of("theme", "dark", "loyaltyEnabled", true)
        );

        assertNotNull(tenant.getId());
        assertEquals("livraria-cultura", tenant.getSlug());
        assertEquals("Livraria Cultura Ltda", tenant.getName());
        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        assertTrue(tenant.isActive());
        assertEquals("dark", tenant.getSettings().get("theme"));
        assertNotNull(tenant.getCreatedAt());
        assertNotNull(tenant.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve normalizar o slug para minúsculas e sem espaços")
    void shouldNormalizeSlug() {
        Tenant tenant = Tenant.create(
                "  Livraria-Central  ",
                "Livraria Central",
                null,
                null,
                "central@livraria.com",
                null,
                null
        );

        assertEquals("livraria-central", tenant.getSlug());
    }

    @Test
    @DisplayName("Deve suspender e reativar tenant corretamente")
    void shouldSuspendAndReactivateTenant() {
        Tenant tenant = Tenant.create(
                "livraria-teste",
                "Livraria Teste",
                null,
                null,
                "teste@livraria.com",
                null,
                null
        );

        tenant.suspend();
        assertEquals(TenantStatus.SUSPENDED, tenant.getStatus());
        assertFalse(tenant.isActive());

        tenant.activate();
        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        assertTrue(tenant.isActive());
    }

    @Test
    @DisplayName("Deve lançar exceção se dados obrigatórios forem nulos")
    void shouldThrowExceptionWhenRequiredFieldsAreNull() {
        assertThrows(NullPointerException.class, () ->
                Tenant.create(null, "Nome", null, null, "email@teste.com", null, null)
        );

        assertThrows(NullPointerException.class, () ->
                Tenant.create("slug", null, null, null, "email@teste.com", null, null)
        );

        assertThrows(NullPointerException.class, () ->
                Tenant.create("slug", "Nome", null, null, null, null, null)
        );
    }
}

