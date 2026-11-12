package com.livraria.ecommerce.domain.model.permission;

import com.livraria.ecommerce.domain.model.user.UserRole;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class RolePermissions {

    private static final Map<UserRole, Set<String>> PERMISSIONS_BY_ROLE = Map.of(
            UserRole.SUPER_ADMIN, Set.of(
                    "CATALOG_READ", "CATALOG_WRITE", "CATALOG_IMPORT",
                    "STOCK_READ", "STOCK_WRITE", "STOCK_TRANSFER",
                    "ORDER_READ", "ORDER_WRITE", "ORDER_CANCEL",
                    "PDV_SELL", "PDV_DISCOUNT", "PDV_CASH_CONTROL",
                    "FISCAL_EMIT_NFE", "FISCAL_EMIT_NFCE",
                    "TENANT_MANAGE", "COMPANY_MANAGE", "USER_MANAGE", "FINANCIAL_VIEW"
            ),
            UserRole.TENANT_ADMIN, Set.of(
                    "CATALOG_READ", "CATALOG_WRITE", "CATALOG_IMPORT",
                    "STOCK_READ", "STOCK_WRITE", "STOCK_TRANSFER",
                    "ORDER_READ", "ORDER_WRITE", "ORDER_CANCEL",
                    "PDV_SELL", "PDV_DISCOUNT", "PDV_CASH_CONTROL",
                    "FISCAL_EMIT_NFE", "FISCAL_EMIT_NFCE",
                    "TENANT_MANAGE", "COMPANY_MANAGE", "USER_MANAGE", "FINANCIAL_VIEW"
            ),
            UserRole.STORE_MANAGER, Set.of(
                    "CATALOG_READ", "CATALOG_WRITE",
                    "STOCK_READ", "STOCK_WRITE", "STOCK_TRANSFER",
                    "ORDER_READ", "ORDER_WRITE",
                    "PDV_SELL", "PDV_DISCOUNT", "PDV_CASH_CONTROL",
                    "FISCAL_EMIT_NFE", "FISCAL_EMIT_NFCE", "FINANCIAL_VIEW"
            ),
            UserRole.ATTENDANT, Set.of(
                    "CATALOG_READ",
                    "STOCK_READ",
                    "ORDER_READ", "ORDER_WRITE",
                    "PDV_SELL", "PDV_CASH_CONTROL",
                    "FISCAL_EMIT_NFCE"
            ),
            UserRole.CUSTOMER, Set.of(
                    "CATALOG_READ",
                    "ORDER_READ", "ORDER_WRITE"
            )
    );

    private RolePermissions() {}

    public static Set<String> getPermissionsForRole(UserRole role) {
        if (role == null) {
            return Collections.emptySet();
        }
        return PERMISSIONS_BY_ROLE.getOrDefault(role, Collections.emptySet());
    }
}

