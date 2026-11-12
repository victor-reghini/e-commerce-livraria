package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.domain.model.catalog.Product;
import com.livraria.ecommerce.domain.model.catalog.ProductType;
import com.livraria.ecommerce.domain.port.in.CatalogUseCase;
import com.livraria.ecommerce.domain.port.out.StockRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - StorefrontController (Catálogo Público e PDP)")
class StorefrontControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CatalogUseCase catalogUseCase;

    @Mock
    private StockRepositoryPort stockRepository;

    @Mock
    private SpringDataCompanyRepository companyRepository;

    @InjectMocks
    private StorefrontController storefrontController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storefrontController).build();
    }

    @Test
    @DisplayName("GET /api/v1/store/products - Deve listar produtos públicos com paginação")
    void shouldListPublicProducts() throws Exception {
        Product prod1 = Product.create(tenantId, null, null, "Clean Code", null, "clean-code", "Livro", ProductType.BOOK);
        Product prod2 = Product.create(tenantId, null, null, "Domain-Driven Design", null, "ddd", "Livro", ProductType.BOOK);

        when(catalogUseCase.searchProducts(eq(tenantId), any(), any(), any(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of(prod1, prod2));
        when(companyRepository.findAllByTenantId(tenantId)).thenReturn(List.of(
                CompanyJpaEntity.builder().id(companyId).name("Matriz").headquarters(true).build()
        ));

        mockMvc.perform(get("/api/v1/store/products")
                        .header("X-Tenant-ID", tenantId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Clean Code"))
                .andExpect(jsonPath("$[1].title").value("Domain-Driven Design"));
    }

    @Test
    @DisplayName("GET /api/v1/store/products/{slug} - Deve retornar PDP por slug")
    void shouldGetProductBySlug() throws Exception {
        Product prod = Product.create(tenantId, null, null, "Clean Code", null, "clean-code", "Livro de Engenharia", ProductType.BOOK);

        when(catalogUseCase.getProductBySlug(eq(tenantId), eq("clean-code"))).thenReturn(prod);
        when(companyRepository.findAllByTenantId(tenantId)).thenReturn(List.of(
                CompanyJpaEntity.builder().id(companyId).name("Matriz").headquarters(true).build()
        ));

        mockMvc.perform(get("/api/v1/store/products/clean-code")
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.slug").value("clean-code"));
    }

    @Test
    @DisplayName("GET /api/v1/store/search?q=clean - Deve retornar sugestões de autocomplete")
    void shouldSearchWithAutocomplete() throws Exception {
        Product prod = Product.create(tenantId, null, null, "Clean Architecture", null, "clean-arch", "Sinopse", ProductType.BOOK);

        when(catalogUseCase.searchProducts(eq(tenantId), eq("clean"), any(), any(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(List.of(prod));
        when(companyRepository.findAllByTenantId(tenantId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/store/search")
                        .header("X-Tenant-ID", tenantId.toString())
                        .param("q", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Clean Architecture"));
    }
}
