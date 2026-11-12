package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.catalog.CatalogDto;
import com.livraria.ecommerce.domain.model.catalog.Author;
import com.livraria.ecommerce.domain.model.catalog.Category;
import com.livraria.ecommerce.domain.model.catalog.Publisher;
import com.livraria.ecommerce.domain.port.in.CatalogUseCase;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/taxonomy")
@Tag(name = "Taxonomia: Categorias, Autores e Editoras", description = "Endpoints para gerenciamento de categorias hierárquicas, autores e editoras")
public class TaxonomyController {

    private final CatalogUseCase catalogUseCase;

    public TaxonomyController(CatalogUseCase catalogUseCase) {
        this.catalogUseCase = catalogUseCase;
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Cadastrar Categoria de Produto")
    public ResponseEntity<Category> createCategory(
            @Valid @RequestBody CatalogDto.CategoryRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(tenantHeader);
        Category category = catalogUseCase.createCategory(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar todas as Categorias do Tenant")
    public ResponseEntity<List<Category>> listCategories(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(tenantHeader);
        return ResponseEntity.ok(catalogUseCase.listCategories(tenantId));
    }

    @PostMapping("/authors")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Cadastrar Autor / Criador")
    public ResponseEntity<Author> createAuthor(
            @Valid @RequestBody CatalogDto.AuthorRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(tenantHeader);
        Author author = catalogUseCase.createAuthor(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(author);
    }

    @GetMapping("/authors")
    @Operation(summary = "Listar todos os Autores do Tenant")
    public ResponseEntity<List<Author>> listAuthors(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(tenantHeader);
        return ResponseEntity.ok(catalogUseCase.listAuthors(tenantId));
    }

    @PostMapping("/publishers")
    @PreAuthorize("hasAnyAuthority('CATALOG_WRITE', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Cadastrar Editora")
    public ResponseEntity<Publisher> createPublisher(
            @Valid @RequestBody CatalogDto.PublisherRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(tenantHeader);
        Publisher publisher = catalogUseCase.createPublisher(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(publisher);
    }

    @GetMapping("/publishers")
    @Operation(summary = "Listar todas as Editoras do Tenant")
    public ResponseEntity<List<Publisher>> listPublishers(
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantHeader) {
        UUID tenantId = resolveTenantId(tenantHeader);
        return ResponseEntity.ok(catalogUseCase.listPublishers(tenantId));
    }

    private UUID resolveTenantId(String tenantHeader) {
        if (tenantHeader != null && !tenantHeader.isBlank()) {
            try {
                return UUID.fromString(tenantHeader);
            } catch (IllegalArgumentException ignored) {}
        }
        return TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }
}
