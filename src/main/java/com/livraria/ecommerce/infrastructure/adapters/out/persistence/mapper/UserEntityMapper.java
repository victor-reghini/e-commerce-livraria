package com.livraria.ecommerce.infrastructure.adapters.out.persistence.mapper;

import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
public class UserEntityMapper {

    public User toDomain(UserJpaEntity entity) {
        return toDomain(entity, Collections.emptyList());
    }

    public User toDomain(UserJpaEntity entity, List<UUID> companyIds) {
        if (entity == null) {
            return null;
        }

        return new User(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.isActive(),
                companyIds != null ? companyIds : Collections.emptyList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserJpaEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        return new UserJpaEntity(
                domain.getId(),
                domain.getTenantId(),
                domain.getName(),
                domain.getEmail(),
                domain.getPasswordHash(),
                domain.getRole(),
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
