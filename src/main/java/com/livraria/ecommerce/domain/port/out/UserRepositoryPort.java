package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    List<User> findAllByTenantId(UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);

    void saveUserCompanies(UUID userId, UUID tenantId, List<UUID> companyIds);

    List<UUID> findCompanyIdsByUserId(UUID userId);
}
