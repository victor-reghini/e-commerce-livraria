package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.ecommerce.BudgetStatus;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudget;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudgetItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorporateBudgetTest {

    private final UUID tenantId = UUID.randomUUID();

    @Test
    @DisplayName("Deve criar orçamento corporativo com status inicial RASCUNHO e calcular totais")
    void shouldCreateCorporateBudget() {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-01", "Livro Corporativo", 10, new BigDecimal("60.00"), BigDecimal.ZERO, new BigDecimal("600.00"));
        CorporateBudget budget = CorporateBudget.create(
                tenantId, "ORC-001", UUID.randomUUID(), "Empresa XPTO Ltda", "XPTO Tech",
                "12.345.678/0001-90", "123456", "Maria Gestora", "compras@xpto.com",
                "(11) 98765-4321", 30, new BigDecimal("50.00"), new BigDecimal("30.00"),
                List.of(item), "Orçamento para biblioteca corporativa"
        );

        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.RASCUNHO);
        assertThat(budget.getSubtotal()).isEqualByComparingTo(new BigDecimal("600.00"));
        assertThat(budget.getDiscount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(budget.getShippingAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(budget.getTotalAmount()).isEqualByComparingTo(new BigDecimal("580.00"));
        assertThat(budget.getPaymentTermsDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("Trava de Inadimplência: Deve bloquear aprovação se cliente possuir faturas vencidas em aberto")
    void shouldBlockApprovalWhenCustomerHasOverdueInvoices() {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-01", "Livro", 5, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("250.00"));
        CorporateBudget budget = CorporateBudget.create(
                tenantId, "ORC-001", UUID.randomUUID(), "Universidade ABC", "Univ ABC",
                "98.765.432/0001-10", "ISENTO", "Reitor Silva", "compras@abc.edu.br",
                null, 30, BigDecimal.ZERO, BigDecimal.ZERO, List.of(item), null
        );

        budget.submitForApproval();
        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.AGUARDANDO_APROVACAO);

        // Tentativa de aprovação com pendência financeira (hasOverdueInvoices = true)
        assertThatThrownBy(() -> budget.approve(true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Trava de Inadimplência ativada");

        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.AGUARDANDO_APROVACAO);
    }

    @Test
    @DisplayName("Deve aprovar orçamento com sucesso quando cliente estiver adimplente")
    void shouldApproveBudgetWhenCustomerIsCompliant() {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-01", "Livro", 5, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("250.00"));
        CorporateBudget budget = CorporateBudget.create(
                tenantId, "ORC-001", UUID.randomUUID(), "Escola Modelo", null,
                "11.222.333/0001-44", null, "Diretor", "contato@modelo.com",
                null, 30, BigDecimal.ZERO, BigDecimal.ZERO, List.of(item), null
        );

        budget.submitForApproval();
        budget.approve(false);

        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.APROVADO);
        assertThat(budget.getApprovedAt()).isNotNull();
        assertThat(budget.getDueDate()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("Deve rejeitar orçamento com motivo")
    void shouldRejectBudgetWithReason() {
        CorporateBudgetItem item = new CorporateBudgetItem(UUID.randomUUID(), UUID.randomUUID(), "SKU-01", "Livro", 1, new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("50.00"));
        CorporateBudget budget = CorporateBudget.create(
                tenantId, "ORC-001", null, "Cliente", null,
                "00.000.000/0001-00", null, "Contato", "c@email.com",
                null, 30, BigDecimal.ZERO, BigDecimal.ZERO, List.of(item), null
        );

        budget.reject("Preço acima do teto orçamentário aprovado pelo conselho");
        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.REJEITADO);
        assertThat(budget.getRejectionReason()).contains("acima do teto orçamentário");
    }
}
