package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.port.out.UserRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.mapper.UserEntityMapper;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository userRepository;
    private final UserEntityMapper mapper;

    public UserPersistenceAdapter(SpringDataUserRepository userRepository, UserEntityMapper mapper) {
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity saved = userRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmailAndTenantId(String email, UUID tenantId) {
        return userRepository.findByEmailIgnoreCaseAndTenantId(email.toLowerCase().trim(), tenantId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmailAndTenantId(String email, UUID tenantId) {
        return userRepository.existsByEmailIgnoreCaseAndTenantId(email.toLowerCase().trim(), tenantId);
    }
}

