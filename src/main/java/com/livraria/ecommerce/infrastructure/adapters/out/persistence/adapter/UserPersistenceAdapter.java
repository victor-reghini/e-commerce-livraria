package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.user.User;
import com.livraria.ecommerce.domain.port.out.UserRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.UserJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.mapper.UserEntityMapper;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataUserRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository userRepository;
    private final UserEntityMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public UserPersistenceAdapter(SpringDataUserRepository userRepository,
                                  UserEntityMapper mapper,
                                  JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity saved = userRepository.save(entity);
        List<UUID> companyIds = findCompanyIdsByUserId(saved.getId());
        return mapper.toDomain(saved, companyIds);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(entity -> {
            List<UUID> companyIds = findCompanyIdsByUserId(entity.getId());
            return mapper.toDomain(entity, companyIds);
        });
    }

    @Override
    public Optional<User> findByEmailAndTenantId(String email, UUID tenantId) {
        UUID effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantId().orElse(null);
        return userRepository.findByEmailIgnoreCaseAndTenantId(email.toLowerCase().trim(), effectiveTenantId)
                .map(entity -> {
                    List<UUID> companyIds = findCompanyIdsByUserId(entity.getId());
                    return mapper.toDomain(entity, companyIds);
                });
    }

    @Override
    public List<User> findAllByTenantId(UUID tenantId) {
        UUID effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantId().orElse(null);
        return userRepository.findAllByTenantId(effectiveTenantId).stream()
                .map(entity -> {
                    List<UUID> companyIds = findCompanyIdsByUserId(entity.getId());
                    return mapper.toDomain(entity, companyIds);
                })
                .toList();
    }

    @Override
    public boolean existsByEmailAndTenantId(String email, UUID tenantId) {
        UUID effectiveTenantId = tenantId != null ? tenantId : TenantContext.getTenantId().orElse(null);
        return userRepository.existsByEmailIgnoreCaseAndTenantId(email.toLowerCase().trim(), effectiveTenantId);
    }

    @Override
    public void saveUserCompanies(UUID userId, UUID tenantId, List<UUID> companyIds) {
        jdbcTemplate.update("DELETE FROM user_companies WHERE user_id = ?", userId);
        if (companyIds != null && !companyIds.isEmpty()) {
            for (UUID companyId : companyIds) {
                jdbcTemplate.update(
                        "INSERT INTO user_companies (user_id, company_id, tenant_id) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                        userId, companyId, tenantId
                );
            }
        }
    }

    @Override
    public List<UUID> findCompanyIdsByUserId(UUID userId) {
        String sql = "SELECT company_id FROM user_companies WHERE user_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("company_id", UUID.class), userId);
    }
}
