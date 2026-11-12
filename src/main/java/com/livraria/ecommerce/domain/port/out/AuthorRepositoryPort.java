package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.catalog.Author;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorRepositoryPort {
    Author save(Author author);
    Optional<Author> findById(UUID id);
    Optional<Author> findByName(UUID tenantId, String name);
    List<Author> findAllByTenantId(UUID tenantId);
    void deleteById(UUID id);
}

