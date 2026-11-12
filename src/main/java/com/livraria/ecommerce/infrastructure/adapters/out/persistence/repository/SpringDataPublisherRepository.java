package com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.PublisherJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPublisherRepository extends JpaRepository<PublisherJpaEntity, UUID> {
    Optional<PublisherJpaEntity> findByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
    List<PublisherJpaEntity> findAllByTenantIdOrderByNameAsc(UUID tenantId);
}

