package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.catalog.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublisherRepositoryPort {
    Publisher save(Publisher publisher);
    Optional<Publisher> findById(UUID id);
    Optional<Publisher> findByName(UUID tenantId, String name);
    List<Publisher> findAllByTenantId(UUID tenantId);
    void deleteById(UUID id);
}

