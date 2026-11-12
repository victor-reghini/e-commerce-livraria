package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.catalog.Sku;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuRepositoryPort {
    Sku save(Sku sku);
    Optional<Sku> findById(UUID id);
    Optional<Sku> findBySkuCode(UUID tenantId, String skuCode);
    Optional<Sku> findByBarcode(UUID tenantId, String barcode);
    Optional<Sku> findByIsbn(UUID tenantId, String isbn);
    List<Sku> findAllByProductId(UUID productId);
    List<Sku> findAllByIds(List<UUID> ids);
    void deleteById(UUID id);
}

