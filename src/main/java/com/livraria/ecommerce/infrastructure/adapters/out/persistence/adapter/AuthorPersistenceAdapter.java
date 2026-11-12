package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.catalog.Author;
import com.livraria.ecommerce.domain.port.out.AuthorRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.AuthorJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataAuthorRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthorPersistenceAdapter implements AuthorRepositoryPort {

    private final SpringDataAuthorRepository repository;

    public AuthorPersistenceAdapter(SpringDataAuthorRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Author save(Author author) {
        AuthorJpaEntity entity = AuthorJpaEntity.builder()
                .id(author.getId())
                .tenantId(author.getTenantId())
                .name(author.getName())
                .biography(author.getBiography())
                .createdAt(author.getCreatedAt())
                .updatedAt(author.getUpdatedAt())
                .build();
        AuthorJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Author> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Author> findByName(UUID tenantId, String name) {
        return repository.findByTenantIdAndNameIgnoreCase(tenantId, name).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Author> findAllByTenantId(UUID tenantId) {
        return repository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private Author toDomain(AuthorJpaEntity entity) {
        return new Author(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getBiography(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

