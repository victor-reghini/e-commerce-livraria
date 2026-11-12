package com.livraria.ecommerce.infrastructure.adapters.out.persistence.adapter;

import com.livraria.ecommerce.domain.model.catalog.Publisher;
import com.livraria.ecommerce.domain.port.out.PublisherRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.PublisherJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataPublisherRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class PublisherPersistenceAdapter implements PublisherRepositoryPort {

    private final SpringDataPublisherRepository repository;

    public PublisherPersistenceAdapter(SpringDataPublisherRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Publisher save(Publisher publisher) {
        PublisherJpaEntity entity = PublisherJpaEntity.builder()
                .id(publisher.getId())
                .tenantId(publisher.getTenantId())
                .name(publisher.getName())
                .createdAt(publisher.getCreatedAt())
                .updatedAt(publisher.getUpdatedAt())
                .build();
        PublisherJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publisher> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Publisher> findByName(UUID tenantId, String name) {
        return repository.findByTenantIdAndNameIgnoreCase(tenantId, name).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Publisher> findAllByTenantId(UUID tenantId) {
        return repository.findAllByTenantIdOrderByNameAsc(tenantId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private Publisher toDomain(PublisherJpaEntity entity) {
        return new Publisher(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}

