package com.livraria.ecommerce.application;

import com.livraria.ecommerce.application.dto.ecommerce.CorporateBudgetDto;
import com.livraria.ecommerce.application.service.CorporateBudgetService;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.ecommerce.BudgetStatus;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudget;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudgetItem;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.port.out.CorporateBudgetRepositoryPort;
import com.livraria.ecommerce.domain.port.out.OrderRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorporateBudgetServiceTest {

    @Mock private CorporateBudgetRepositoryPort budgetRepository;
    @Mock private OrderRepositoryPort orderRepository;
    @Mock private SpringDataSkuRepository skuRepository;
    @Mock private SpringDataProductRepository productRepository;
    @Mock private SpringDataStockItemRepository stockItemRepository;
    @Mock private SpringDataStockMovementRepository stockMovementRepository;
    @Mock private SpringDataCompanyRepository companyRepository;

    private CorporateBudgetService budgetService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID budgetId = UUID.randomUUID();
    private final UUID skuId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        budgetService = new CorporateBudgetService(
                budgetRepository, orderRepository, skuRepository, productRepository,
                stockItemRepository, stockMovementRepository, companyRepository
        );
    }

    @Test
    @DisplayName("Deve criar orçamento corporativo com itens")
    void shouldCreateCorporateBudget() {
        SkuJpaEntity sku = SkuJpaEntity.builder().id(skuId).productId(productId).skuCode("SKU-CORP").price(new BigDecimal("70.00")).build();
        ProductJpaEntity prod = ProductJpaEntity.builder().id(productId).title("Livro Técnico").build();
        when(skuRepository.findById(skuId)).thenReturn(Optional.of(sku));
        when(productRepository.findById(productId)).thenReturn(Optional.of(prod));
        when(budgetRepository.save(any(CorporateBudget.class))).thenAnswer(i -> i.getArgument(0));

        CorporateBudgetDto.BudgetItemRequest item = new CorporateBudgetDto.BudgetItemRequest(skuId, 5, null, null);
        CorporateBudgetDto.CreateBudgetRequest request = new CorporateBudgetDto.CreateBudgetRequest(
                UUID.randomUUID(), "Tech Corp", "Tech", "12.345.678/0001-90", null,
                "Gerente", "gerente@tech.com", null, 30, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(item), "Orçamento corporativo");

        CorporateBudget budget = budgetService.createBudget(tenantId, request);

        assertThat(budget.getCorporateName()).isEqualTo("Tech Corp");
        assertThat(budget.getCnpj()).isEqualTo("12345678000190");
        assertThat(budget.getTotalAmount()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.RASCUNHO);
        verify(budgetRepository).save(any(CorporateBudget.class));
    }

    @Test
    @DisplayName("Trava de Inadimplência: Deve lançar BusinessException e bloquear aprovação de orçamento se houver faturas vencidas")
    void shouldBlockApprovalDueToDefaultLock() {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), skuId, "SKU-CORP", "Livro", 5, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("250.00"));
        CorporateBudget budget = CorporateBudget.create(tenantId, "ORC-1", null, "Universidade ABC", null,
                "99.888.777/0001-66", null, "Reitor", "reitor@abc.edu.br", null, 30, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(item), null);
        budget.submitForApproval();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        // Trava de inadimplência ativada pelo repositório
        when(orderRepository.hasOverdueInvoices(tenantId, "99888777000166")).thenReturn(true);

        assertThatThrownBy(() -> budgetService.approveBudget(tenantId, budgetId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trava de Inadimplência ativada");

        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.AGUARDANDO_APROVACAO);
    }

    @Test
    @DisplayName("Deve aprovar orçamento com sucesso quando cliente estiver totalmente adimplente")
    void shouldApproveBudgetSuccessfully() {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), skuId, "SKU-CORP", "Livro", 5, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("250.00"));
        CorporateBudget budget = CorporateBudget.create(tenantId, "ORC-1", null, "Cliente Bom Pagador", null,
                "11.222.333/0001-44", null, "Financeiro", "fin@bom.com.br", null, 30, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(item), null);
        budget.submitForApproval();

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));
        when(orderRepository.hasOverdueInvoices(tenantId, "11222333000144")).thenReturn(false);
        when(budgetRepository.save(any(CorporateBudget.class))).thenAnswer(i -> i.getArgument(0));

        CorporateBudget approved = budgetService.approveBudget(tenantId, budgetId);

        assertThat(approved.getStatus()).isEqualTo(BudgetStatus.APROVADO);
        assertThat(approved.getDueDate()).isEqualTo(LocalDate.now().plusDays(30));
        verify(budgetRepository).save(approved);
    }

    @Test
    @DisplayName("Deve faturar orçamento aprovado: cria pedido a prazo com boleto de 30 dias e debita estoque")
    void shouldInvoiceApprovedBudget() {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), skuId, "SKU-CORP", "Livro", 2, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("100.00"));
        CorporateBudget budget = CorporateBudget.create(tenantId, "ORC-1", null, "Empresa", null,
                "11.222.333/0001-44", null, "Financeiro", "fin@email.com", null, 30, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(item), null);
        budget.submitForApproval();
        budget.approve(false);

        when(budgetRepository.findById(budgetId)).thenReturn(Optional.of(budget));

        CompanyJpaEntity matriz = CompanyJpaEntity.builder().id(companyId).name("Matriz").headquarters(true).build();
        when(companyRepository.findAllByTenantId(tenantId)).thenReturn(List.of(matriz));

        StockItemJpaEntity stock = StockItemJpaEntity.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .companyId(companyId).skuId(skuId).quantity(10).reservedQuantity(0).build();
        when(stockItemRepository.findByTenantIdAndCompanyIdAndSkuId(tenantId, companyId, skuId)).thenReturn(Optional.of(stock));

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        when(budgetRepository.save(any(CorporateBudget.class))).thenAnswer(i -> i.getArgument(0));

        Order order = budgetService.invoiceBudget(tenantId, budgetId);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FATURADO_A_PRAZO);
        assertThat(stock.getQuantity()).isEqualTo(8); // Baixa no estoque
        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.FATURADO);
        assertThat(budget.getInvoicedOrderId()).isEqualTo(order.getId());
        verify(stockItemRepository).save(stock);
        verify(orderRepository).save(any(Order.class));
    }
}

