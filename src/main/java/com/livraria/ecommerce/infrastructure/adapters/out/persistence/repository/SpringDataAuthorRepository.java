package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.AuthorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAuthorRepository extends JpaRepository<AuthorJpaEntity, UUID> {
    Optional<AuthorJpaEntity> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
    List<AuthorJpaEntity> findAllByTenantIdOrderByNameAsc(UUID tenantId);
}

