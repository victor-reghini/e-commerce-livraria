package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.catalog.Category;
import com.livraria.ecommerce.domain.port.out.CategoryRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CategoryJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCategoryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryPersistenceAdapter implements CategoryRepositoryPort {

    private final SpringDataCategoryRepository repository;

    public CategoryPersistenceAdapter(SpringDataCategoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Category save(Category category) {
        CategoryJpaEntity entity = toEntity(category);
        CategoryJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findBySlug(UUID tenantId, String slug) {
        return repository.findByTenantIdAndSlug(tenantId, slug).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> findAllByTenantId(UUID tenantId) {
        return repository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private CategoryJpaEntity toEntity(Category domain) {
        return CategoryJpaEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId())
                .name(domain.getName())
                .slug(domain.getSlug())
                .parentId(domain.getParentId())
                .active(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private Category toDomain(CategoryJpaEntity entity) {
        return new Category(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getSlug(),
                entity.getParentId(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

