package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.catalog.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {
    Category save(Category category);
    Optional<Category> findById(UUID id);
    Optional<Category> findBySlug(UUID tenantId, String slug);
    List<Category> findAllByTenantId(UUID tenantId);
    void deleteById(UUID id);
}

