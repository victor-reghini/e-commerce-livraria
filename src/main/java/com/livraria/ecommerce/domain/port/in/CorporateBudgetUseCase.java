package com.livraria.ecommerce.domain.port.in;

import com.livraria.ecommerce.application.dto.ecommerce.CorporateBudgetDto;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudget;
import com.livraria.ecommerce.domain.model.ecommerce.Order;

import java.util.List;
import java.util.UUID;

public interface CorporateBudgetUseCase {

    CorporateBudget createBudget(UUID tenantId, CorporateBudgetDto.CreateBudgetRequest request);

    CorporateBudget getBudget(UUID tenantId, UUID budgetId);

    List<CorporateBudget> listBudgets(UUID tenantId, int page, int size);

    CorporateBudget submitForApproval(UUID tenantId, UUID budgetId);

    /**
     * Aprova o orçamento aplicando a Trava de Inadimplência automática.
     */
    CorporateBudget approveBudget(UUID tenantId, UUID budgetId);

    CorporateBudget rejectBudget(UUID tenantId, UUID budgetId, String reason);

    /**
     * Converte o orçamento aprovado em um pedido faturado a prazo (ex.: 30 dias),
     * debitando o estoque e gerando o boleto faturado.
     */
    Order invoiceBudget(UUID tenantId, UUID budgetId);
}

