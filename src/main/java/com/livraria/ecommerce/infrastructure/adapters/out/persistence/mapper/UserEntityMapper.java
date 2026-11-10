package com.livraria.ecommerce.infrastructure.adapters.out.persistence.mapper;

import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserEntityMapper {

    public User toDomain(UserJpaEntity entity) {
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

