package com.livraria.ecommerce.application.dto;

import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.model.user.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserDto(
        UUID id,
        UUID tenantId,
        String name,
        String email,
        UserRole role,
        boolean active,
        List<UUID> companyIds,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserDto fromDomain(User user) {
        return new UserDto(
                user.getId(),
                user.getTenantId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCompanyIds(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

