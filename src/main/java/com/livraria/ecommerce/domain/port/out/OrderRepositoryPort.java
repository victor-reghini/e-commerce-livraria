package com.livraria.ecommerce.domain.port.out;

import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {
    Optional<Order> findById(UUID id);
    Optional<Order> findByTenantIdAndOrderNumber(UUID tenantId, String orderNumber);
    List<Order> findAllByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
    List<Order> findAllByTenantId(UUID tenantId, int page, int size);
    List<Order> findAllByTenantIdAndStatus(UUID tenantId, OrderStatus status);
    
    /**
     * Trava de Inadimplência:
     * Verifica se o cliente (por CPF/CNPJ) possui pedidos faturados a prazo ou pendentes
     * com boletos ou pagamentos em aberto já vencidos.
     */
    boolean hasOverdueInvoices(UUID tenantId, String customerDocument);

    Order save(Order order);
}
