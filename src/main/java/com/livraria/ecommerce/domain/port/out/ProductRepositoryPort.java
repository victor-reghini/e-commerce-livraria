package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.catalog.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    Optional<Product> findBySlug(UUID tenantId, String slug);
    List<Product> search(UUID tenantId, String query, UUID categoryId, UUID publisherId, Boolean active, int page, int size);
    long countSearch(UUID tenantId, String query, UUID categoryId, UUID publisherId, Boolean active);
    void deleteById(UUID id);
}

