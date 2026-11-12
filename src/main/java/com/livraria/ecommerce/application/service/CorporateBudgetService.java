package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.application.dto.ecommerce.CorporateBudgetDto;
import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudget;
import com.livraria.ecommerce.domain.model.ecommerce.CorporateBudgetItem;
import com.livraria.ecommerce.domain.model.ecommerce.EcommercePaymentMethod;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderItem;
import com.livraria.ecommerce.domain.model.ecommerce.OrderPayment;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentGateway;
import com.livraria.ecommerce.domain.model.ecommerce.PaymentStatus;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingAddress;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingCarrier;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingService;
import com.livraria.ecommerce.domain.model.stock.MovementType;
import com.livraria.ecommerce.domain.port.in.CorporateBudgetUseCase;
import com.livraria.ecommerce.domain.port.out.CorporateBudgetRepositoryPort;
import com.livraria.ecommerce.domain.port.out.OrderRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.CompanyJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.ProductJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.SkuJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockItemJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.entity.StockMovementJpaEntity;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataCompanyRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataProductRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataSkuRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockItemRepository;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.repository.SpringDataStockMovementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CorporateBudgetService implements CorporateBudgetUseCase {

    private static final Logger log = LoggerFactory.getLogger(CorporateBudgetService.class);

    private final CorporateBudgetRepositoryPort budgetRepository;
    private final OrderRepositoryPort orderRepository;
    private final SpringDataSkuRepository skuRepository;
    private final SpringDataProductRepository productRepository;
    private final SpringDataStockItemRepository stockItemRepository;
    private final SpringDataStockMovementRepository stockMovementRepository;
    private final SpringDataCompanyRepository companyRepository;

    public CorporateBudgetService(CorporateBudgetRepositoryPort budgetRepository,
                                  OrderRepositoryPort orderRepository,
                                  SpringDataSkuRepository skuRepository,
                                  SpringDataProductRepository productRepository,
                                  SpringDataStockItemRepository stockItemRepository,
                                  SpringDataStockMovementRepository stockMovementRepository,
                                  SpringDataCompanyRepository companyRepository) {
        this.budgetRepository = budgetRepository;
        this.orderRepository = orderRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.stockItemRepository = stockItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional
    public CorporateBudget createBudget(UUID tenantId, CorporateBudgetDto.CreateBudgetRequest request) {
        String budgetNumber = "ORC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 9999));

        List<CorporateBudgetItem> items = new ArrayList<>();
        for (CorporateBudgetDto.BudgetItemRequest itemReq : request.items()) {
            SkuJpaEntity sku = skuRepository.findById(itemReq.skuId())
                    .orElseThrow(() -> new BusinessException("SKU não encontrado com ID: " + itemReq.skuId()));
            ProductJpaEntity product = productRepository.findById(sku.getProductId())
                    .orElseThrow(() -> new BusinessException("Produto não encontrado para o SKU"));

            BigDecimal unitPrice = itemReq.customUnitPrice() != null ? itemReq.customUnitPrice() : sku.getPrice();
            BigDecimal discount = itemReq.discount() != null ? itemReq.discount() : BigDecimal.ZERO;
            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity())).subtract(discount);

            items.add(new CorporateBudgetItem(
                    UUID.randomUUID(),
                    sku.getId(),
                    sku.getSkuCode(),
                    product.getTitle(),
                    itemReq.quantity(),
                    unitPrice,
                    discount,
                    total
            ));
        }

        CorporateBudget budget = CorporateBudget.create(
                tenantId,
                budgetNumber,
                request.customerId(),
                request.corporateName(),
                request.tradeName(),
                request.cnpj(),
                request.stateRegistration(),
                request.contactName(),
                request.contactEmail(),
                request.contactPhone(),
                request.paymentTermsDays(),
                request.discount(),
                request.shippingAmount(),
                items,
                request.notes()
        );

        return budgetRepository.save(budget);
    }

    @Override
    @Transactional(readOnly = true)
    public CorporateBudget getBudget(UUID tenantId, UUID budgetId) {
        return budgetRepository.findById(budgetId)
                .filter(b -> b.getTenantId().equals(tenantId))
                .orElseThrow(() -> new BusinessException("Orçamento corporativo não encontrado com ID: " + budgetId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorporateBudget> listBudgets(UUID tenantId, int page, int size) {
        return budgetRepository.findAllByTenantId(tenantId, page, size);
    }

    @Override
    @Transactional
    public CorporateBudget submitForApproval(UUID tenantId, UUID budgetId) {
        CorporateBudget budget = getBudget(tenantId, budgetId);
        budget.submitForApproval();
        return budgetRepository.save(budget);
    }

    /**
     * Aprovação com Trava de Inadimplência Obrigatória:
     * O cliente é terminantemente bloqueado de aprovar novos orçamentos se possuir
     * qualquer pedido faturado pendente e vencido até que ocorra a quitação integral.
     */
    @Override
    @Transactional
    public CorporateBudget approveBudget(UUID tenantId, UUID budgetId) {
        CorporateBudget budget = getBudget(tenantId, budgetId);

        // Verificar se há faturas em aberto vencidas para este CNPJ
        boolean hasOverdueInvoices = orderRepository.hasOverdueInvoices(tenantId, budget.getCnpj());
        log.info("Verificação de Inadimplência para CNPJ {}: pendências={}", budget.getCnpj(), hasOverdueInvoices);

        // O domínio dispara BusinessException se hasOverdueInvoices for true
        budget.approve(hasOverdueInvoices);

        return budgetRepository.save(budget);
    }

    @Override
    @Transactional
    public CorporateBudget rejectBudget(UUID tenantId, UUID budgetId, String reason) {
        CorporateBudget budget = getBudget(tenantId, budgetId);
        budget.reject(reason);
        return budgetRepository.save(budget);
    }

    /**
     * Faturamento do Orçamento Aprovado:
     * Converte em pedido formal faturado a prazo, dá baixa no estoque e gera boleto a prazo (ex.: 30 dias).
     */
    @Override
    @Transactional
    public Order invoiceBudget(UUID tenantId, UUID budgetId) {
        CorporateBudget budget = getBudget(tenantId, budgetId);
        if (budget.getStatus() != com.livraria.ecommerce.domain.model.ecommerce.BudgetStatus.APROVADO) {
            throw new BusinessException("Apenas orçamentos aprovados podem ser faturados.");
        }

        CompanyJpaEntity fulfillmentCompany = companyRepository.findAllByTenantId(tenantId).stream()
                .filter(CompanyJpaEntity::isHeadquarters)
                .findFirst()
                .orElseGet(() -> companyRepository.findAllByTenantId(tenantId).stream().findFirst()
                        .orElseThrow(() -> new BusinessException("Nenhuma empresa configurada para expedição.")));

        // Verificar e debitar estoque para os itens do orçamento
        for (CorporateBudgetItem item : budget.getItems()) {
            StockItemJpaEntity stock = stockItemRepository
                    .findByTenantIdAndCompanyIdAndSkuId(tenantId, fulfillmentCompany.getId(), item.getSkuId())
                    .orElseThrow(() -> new BusinessException("Estoque não localizado para: " + item.getProductTitle()));
            if (stock.getQuantity() - stock.getReservedQuantity() < item.getQuantity()) {
                throw new BusinessException("Estoque insuficiente para faturar item: " + item.getProductTitle());
            }
            stock.setQuantity(stock.getQuantity() - item.getQuantity());
            stock.setUpdatedAt(Instant.now());
            stockItemRepository.save(stock);

            StockMovementJpaEntity movement = StockMovementJpaEntity.builder()
                    .id(UUID.randomUUID())
                    .tenantId(tenantId)
                    .companyId(fulfillmentCompany.getId())
                    .skuId(item.getSkuId())
                    .movementType(MovementType.VENDA_ECOMMERCE.name())
                    .quantityChange(-item.getQuantity())
                    .balanceAfter(stock.getQuantity())
                    .referenceId(budget.getId())
                    .reason("Faturamento a Prazo Orçamento " + budget.getBudgetNumber())
                    .createdAt(Instant.now())
                    .build();
            stockMovementRepository.save(movement);
        }

        // Criar Pedido correspondente
        String orderNumber = "PED-FAT-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 9999));

        List<OrderItem> orderItems = budget.getItems().stream().map(i -> new OrderItem(
                UUID.randomUUID(),
                i.getSkuId(),
                i.getSkuCode(),
                i.getProductTitle(),
                i.getQuantity(),
                i.getUnitPrice(),
                i.getDiscount(),
                i.getTotal(),
                "4901.99.00",
                "5102"
        )).toList();

        ShippingAddress dummyAddress = new ShippingAddress("Endereço Corporativo", "S/N", null, "Centro", "São Paulo", "SP", "01001000");

        Order order = new Order(
                UUID.randomUUID(),
                tenantId,
                orderNumber,
                budget.getCustomerId(),
                budget.getCorporateName(),
                budget.getContactEmail(),
                budget.getCnpj(),
                budget.getContactPhone(),
                fulfillmentCompany.getId(),
                OrderStatus.FATURADO_A_PRAZO,
                budget.getSubtotal(),
                budget.getDiscount(),
                budget.getShippingAmount(),
                budget.getTotalAmount(),
                ShippingCarrier.CORREIOS,
                ShippingService.SEDEX,
                null,
                null,
                dummyAddress,
                orderItems,
                new ArrayList<>(),
                "Faturado a prazo conforme orçamento " + budget.getBudgetNumber() + " - Prazo " + budget.getPaymentTermsDays() + " dias",
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                null
        );

        // Criar Pagamento Faturado a Prazo (com data de vencimento)
        OrderPayment payment = new OrderPayment(
                UUID.randomUUID(),
                EcommercePaymentMethod.FATURAMENTO_A_PRAZO,
                PaymentGateway.FATURAMENTO_INTERNO,
                "fat_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16),
                PaymentStatus.PENDENTE,
                budget.getTotalAmount(),
                1,
                null,
                null,
                "34191" + String.format("%010d", System.currentTimeMillis() % 10000000000L) + "9876543210",
                "https://sistema.livraria.com/faturas/" + order.getId() + "/boleto.pdf",
                budget.getDueDate(),
                null,
                null,
                Instant.now(),
                Instant.now(),
                null
        );
        order.addPayment(payment);

        orderRepository.save(order);

        // Atualizar orçamento para FATURADO
        budget.markAsInvoiced(order.getId());
        budgetRepository.save(budget);

        log.info("Orçamento {} faturado com sucesso em pedido {}. Vencimento: {}",
                budget.getBudgetNumber(), order.getOrderNumber(), budget.getDueDate());
        return order;
    }
}
