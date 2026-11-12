package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.model.permission.RolePermissions;
import com.livraria.ecommerce.domain.model.user.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RolePermissionsTest {

    @Test
    @DisplayName("TENANT_ADMIN e SUPER_ADMIN devem ter todas as permissões administrativas")
    void adminRolesShouldHaveAllPermissions() {
        Set<String> adminPerms = RolePermissions.getPermissionsForRole(UserRole.TENANT_ADMIN);
        assertTrue(adminPerms.contains("CATALOG_WRITE"));
        assertTrue(adminPerms.contains("USER_MANAGE"));
        assertTrue(adminPerms.contains("COMPANY_MANAGE"));
        assertTrue(adminPerms.contains("FISCAL_EMIT_NFE"));
        assertTrue(adminPerms.contains("STOCK_TRANSFER"));

        Set<String> superAdminPerms = RolePermissions.getPermissionsForRole(UserRole.SUPER_ADMIN);
        assertEquals(adminPerms, superAdminPerms);
    }

    @Test
    @DisplayName("ATTENDANT de PDV deve ter permissões de venda mas não de gestão de usuários")
    void attendantRoleShouldHavePdvPermissionsOnly() {
        Set<String> perms = RolePermissions.getPermissionsForRole(UserRole.ATTENDANT);
        assertTrue(perms.contains("PDV_SELL"));
        assertTrue(perms.contains("PDV_CASH_CONTROL"));
        assertTrue(perms.contains("FISCAL_EMIT_NFCE"));
        assertFalse(perms.contains("USER_MANAGE"));
        assertFalse(perms.contains("TENANT_MANAGE"));
        assertFalse(perms.contains("COMPANY_MANAGE"));
    }

    @Test
    @DisplayName("CUSTOMER de loja virtual deve ter apenas permissões de catálogo e pedidos")
    void customerRoleShouldHaveMinimalPermissions() {
        Set<String> perms = RolePermissions.getPermissionsForRole(UserRole.CUSTOMER);
        assertTrue(perms.contains("CATALOG_READ"));
        assertTrue(perms.contains("ORDER_READ"));
        assertTrue(perms.contains("ORDER_WRITE"));
        assertFalse(perms.contains("PDV_SELL"));
        assertFalse(perms.contains("STOCK_WRITE"));
    }
}

