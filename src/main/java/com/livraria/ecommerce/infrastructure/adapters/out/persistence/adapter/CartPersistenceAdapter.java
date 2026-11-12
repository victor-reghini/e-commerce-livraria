package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.ecommerce.Cart;
import com.livraria.ecommerce.domain.model.ecommerce.CartItem;
import com.livraria.ecommerce.domain.port.out.CartRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CartItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CartJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCartItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCartRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class CartPersistenceAdapter implements CartRepositoryPort {

    private final SpringDataCartRepository cartRepository;
    private final SpringDataCartItemRepository cartItemRepository;
    private final SpringDataSkuRepository skuRepository;
    private final SpringDataProductRepository productRepository;

    public CartPersistenceAdapter(SpringDataCartRepository cartRepository,
                                  SpringDataCartItemRepository cartItemRepository,
                                  SpringDataSkuRepository skuRepository,
                                  SpringDataProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cart> findById(UUID id) {
        return cartRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cart> findByTenantIdAndSessionId(UUID tenantId, String sessionId) {
        return cartRepository.findByTenantIdAndSessionId(tenantId, sessionId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cart> findByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        return cartRepository.findByTenantIdAndCustomerId(tenantId, customerId).map(this::toDomain);
    }

    @Override
    @Transactional
    public Cart save(Cart cart) {
        CartJpaEntity entity = CartJpaEntity.builder()
                .id(cart.getId())
                .tenantId(cart.getTenantId())
                .sessionId(cart.getSessionId())
                .customerId(cart.getCustomerId())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
        cartRepository.save(entity);

        // Atualizar itens
        cartItemRepository.deleteAllByTenantIdAndCartId(cart.getTenantId(), cart.getId());
        List<CartItemJpaEntity> itemEntities = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            itemEntities.add(CartItemJpaEntity.builder()
                    .id(item.getId())
                    .tenantId(cart.getTenantId())
                    .cart(entity)
                    .skuId(item.getSkuId())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .createdAt(cart.getCreatedAt())
                    .updatedAt(cart.getUpdatedAt())
                    .build());
        }
        if (!itemEntities.isEmpty()) {
            cartItemRepository.saveAll(itemEntities);
        }
        return cart;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        cartRepository.deleteById(id);
    }

    private Cart toDomain(CartJpaEntity entity) {
        List<CartItemJpaEntity> itemEntities = entity.getItems() != null ? entity.getItems() : List.of();
        List<UUID> skuIds = itemEntities.stream().map(CartItemJpaEntity::getSkuId).toList();

        Map<UUID, SkuJpaEntity> skuMap = new HashMap<>();
        Map<UUID, String> productTitleMap = new HashMap<>();
        if (!skuIds.isEmpty()) {
            List<SkuJpaEntity> skus = skuRepository.findAllById(skuIds);
            for (SkuJpaEntity sku : skus) {
                skuMap.put(sku.getId(), sku);
            }
            List<UUID> productIds = skus.stream().map(SkuJpaEntity::getProductId).toList();
            List<ProductJpaEntity> products = productRepository.findAllById(productIds);
            for (ProductJpaEntity prod : products) {
                productTitleMap.put(prod.getId(), prod.getTitle());
            }
        }

        List<CartItem> domainItems = new ArrayList<>();
        for (CartItemJpaEntity itemEntity : itemEntities) {
            SkuJpaEntity sku = skuMap.get(itemEntity.getSkuId());
            String skuCode = sku != null ? sku.getSkuCode() : "SKU-" + itemEntity.getSkuId().toString().substring(0, 8);
            String title = (sku != null && productTitleMap.containsKey(sku.getProductId()))
                    ? productTitleMap.get(sku.getProductId())
                    : "Produto";

            domainItems.add(new CartItem(
                    itemEntity.getId(),
                    itemEntity.getSkuId(),
                    skuCode,
                    title,
                    itemEntity.getQuantity(),
                    itemEntity.getUnitPrice()
            ));
        }

        return new Cart(
                entity.getId(),
                entity.getTenantId(),
                entity.getSessionId(),
                entity.getCustomerId(),
                domainItems,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
