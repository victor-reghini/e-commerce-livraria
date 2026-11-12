package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.model.user.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("Deve criar usuário ativo associado ao tenant")
    void shouldCreateActiveUserWithTenant() {
        UUID tenantId = UUID.randomUUID();
        User user = User.create(
                tenantId,
                "João Operador",
                "joao@livraria.com",
                "$2a$10$hashedPasswordHere",
                UserRole.ATTENDANT
        );

        assertNotNull(user.getId());
        assertEquals(tenantId, user.getTenantId());
        assertEquals("João Operador", user.getName());
        assertEquals("joao@livraria.com", user.getEmail());
        assertEquals(UserRole.ATTENDANT, user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("Deve permitir desativação e ativação do usuário")
    void shouldToggleUserActiveState() {
        UUID tenantId = UUID.randomUUID();
        User user = User.create(
                tenantId,
                "Maria Gestora",
                "maria@livraria.com",
                "$2a$10$hash",
                UserRole.STORE_MANAGER
        );

        user.deactivate();
        assertFalse(user.isActive());

        user.activate();
        assertTrue(user.isActive());
    }
}
