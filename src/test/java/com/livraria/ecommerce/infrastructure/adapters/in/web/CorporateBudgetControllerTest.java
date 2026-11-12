package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.livraria.ecommerce.application.dto.ecommerce.CorporateBudgetDto;
import com.livraria.ecommerce.domain.model.ecommerce.BudgetStatus;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudget;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudgetItem;
import com.livraria.ecommerce.domain.port.in.CorporateBudgetUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes de Integração Web - CorporateBudgetController (Orçamentos e Trava de Inadimplência)")
class CorporateBudgetControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private CorporateBudgetUseCase budgetUseCase;

    @InjectMocks
    private CorporateBudgetController budgetController;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID budgetId = UUID.randomUUID();
    private final UUID skuId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(budgetController).build();
    }

    @Test
    @DisplayName("POST /api/v1/budgets - Deve criar orçamento corporativo")
    void shouldCreateBudget() throws Exception {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), skuId, "SKU-1", "Livro", 5, new BigDecimal("60.00"), BigDecimal.ZERO, new BigDecimal("300.00"));
        CorporateBudget budget = CorporateBudget.create(tenantId, "ORC-20261112-0001", null, "Universidade Federal", "UF",
                "12.345.678/0001-99", "ISENTO", "Diretor", "compras@uf.edu.br", null, 30, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(item), "Orçamento acadêmico");

        when(budgetUseCase.createBudget(eq(tenantId), any())).thenReturn(budget);

        CorporateBudgetDto.BudgetItemRequest itemReq = new CorporateBudgetDto.BudgetItemRequest(skuId, 5, new BigDecimal("60.00"), BigDecimal.ZERO);
        CorporateBudgetDto.CreateBudgetRequest req = new CorporateBudgetDto.CreateBudgetRequest(
                null, "Universidade Federal", "UF", "12.345.678/0001-99", "ISENTO",
                "Diretor", "compras@uf.edu.br", null, 30, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(itemReq), "Orçamento acadêmico");

        mockMvc.perform(post("/api/v1/budgets")
                        .header("X-Tenant-ID", tenantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.budgetNumber").value("ORC-20261112-0001"))
                .andExpect(jsonPath("$.corporateName").value("Universidade Federal"))
                .andExpect(jsonPath("$.totalAmount").value(300.00));
    }

    @Test
    @DisplayName("POST /api/v1/budgets/{id}/approve - Deve aprovar orçamento corporativo")
    void shouldApproveBudget() throws Exception {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), skuId, "SKU-1", "Livro", 5, new BigDecimal("60.00"), BigDecimal.ZERO, new BigDecimal("300.00"));
        CorporateBudget budget = CorporateBudget.create(tenantId, "ORC-1", null, "Empresa", null,
                "12345678000199", null, "Gestor", "g@email.com", null, 30, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(item), null);
        budget.submitForApproval();
        budget.approve(false);

        when(budgetUseCase.approveBudget(eq(tenantId), eq(budgetId))).thenReturn(budget);

        mockMvc.perform(post("/api/v1/budgets/{id}/approve", budgetId)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    @DisplayName("GET /api/v1/budgets/{id} - Deve retornar espelho do orçamento")
    void shouldGetBudget() throws Exception {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), skuId, "SKU-1", "Livro", 5, new BigDecimal("60.00"), BigDecimal.ZERO, new BigDecimal("300.00"));
        CorporateBudget budget = CorporateBudget.create(tenantId, "ORC-1", null, "Empresa", null,
                "12345678000199", null, "Gestor", "g@email.com", null, 30, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(item), null);

        when(budgetUseCase.getBudget(eq(tenantId), eq(budgetId))).thenReturn(budget);

        mockMvc.perform(get("/api/v1/budgets/{id}", budgetId)
                        .header("X-Tenant-ID", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetNumber").value("ORC-1"));
    }
}

