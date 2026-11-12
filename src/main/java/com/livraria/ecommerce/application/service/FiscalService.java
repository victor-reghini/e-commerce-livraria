package com.livraria.ecommerce.application.service;

import com.livraria.ecommerce.domain.exception.BusinessException;
import com.livraria.ecommerce.domain.exception.EntityNotFoundException;
import com.livraria.ecommerce.domain.model.company.Address;
import com.livraria.ecommerce.domain.model.company.Company;
import com.livraria.ecommerce.domain.model.ecommerce.Order;
import com.livraria.ecommerce.domain.model.ecommerce.OrderItem;
import com.livraria.ecommerce.domain.model.ecommerce.OrderStatus;
import com.livraria.ecommerce.domain.model.fiscal.FiscalCancellationResult;
import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import com.livraria.ecommerce.domain.model.fiscal.FiscalStatus;
import com.livraria.ecommerce.domain.model.fiscal.SefazAuthResult;
import com.livraria.ecommerce.domain.model.fiscal.SefazStatusResult;
import com.livraria.ecommerce.domain.model.pos.NfceStatus;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import com.livraria.ecommerce.domain.port.in.FiscalUseCase;
import com.livraria.ecommerce.domain.port.out.CompanyRepositoryPort;
import com.livraria.ecommerce.domain.port.out.FiscalRepositoryPort;
import com.livraria.ecommerce.domain.port.out.OrderRepositoryPort;
import com.livraria.ecommerce.domain.port.out.PosSaleRepositoryPort;
import com.livraria.ecommerce.domain.port.out.SefazGatewayPort;
import com.livraria.ecommerce.infrastructure.adapters.out.fiscal.DanfeHtmlGenerator;
import com.livraria.ecommerce.infrastructure.adapters.out.fiscal.NfeXmlBuilder;
import com.livraria.ecommerce.infrastructure.adapters.out.fiscal.SefazAccessKeyGenerator;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FiscalService implements FiscalUseCase {

    private static final Logger log = LoggerFactory.getLogger(FiscalService.class);

    private final FiscalRepositoryPort fiscalRepository;
    private final SefazGatewayPort sefazGateway;
    private final SefazAccessKeyGenerator accessKeyGenerator;
    private final NfeXmlBuilder nfeXmlBuilder;
    private final DanfeHtmlGenerator danfeHtmlGenerator;
    private final OrderRepositoryPort orderRepository;
    private final PosSaleRepositoryPort posSaleRepository;
    private final CompanyRepositoryPort companyRepository;
    private final AuditUseCase auditUseCase;
    private final ObservabilityMetricsService metricsService;

    public FiscalService(
            FiscalRepositoryPort fiscalRepository,
            SefazGatewayPort sefazGateway,
            SefazAccessKeyGenerator accessKeyGenerator,
            NfeXmlBuilder nfeXmlBuilder,
            DanfeHtmlGenerator danfeHtmlGenerator,
            OrderRepositoryPort orderRepository,
            PosSaleRepositoryPort posSaleRepository,
            CompanyRepositoryPort companyRepository,
            AuditUseCase auditUseCase,
            ObservabilityMetricsService metricsService) {
        this.fiscalRepository = fiscalRepository;
        this.sefazGateway = sefazGateway;
        this.accessKeyGenerator = accessKeyGenerator;
        this.nfeXmlBuilder = nfeXmlBuilder;
        this.danfeHtmlGenerator = danfeHtmlGenerator;
        this.orderRepository = orderRepository;
        this.posSaleRepository = posSaleRepository;
        this.companyRepository = companyRepository;
        this.auditUseCase = auditUseCase;
        this.metricsService = metricsService;
    }

    @Override
    @Transactional
    public FiscalInvoice emitNfeForOrder(UUID orderId, int series) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + orderId));

        if (order.getStatus() == OrderStatus.CANCELADO) {
            throw new BusinessException("Não é permitido emitir NF-e para pedidos cancelados.");
        }

        // Idempotência: verificar se já existe NF-e autorizada para o pedido
        Optional<FiscalInvoice> existingOpt = fiscalRepository.findByOrderId(orderId);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == FiscalStatus.AUTORIZADA) {
            return existingOpt.get();
        }

        UUID tenantId = order.getTenantId();
        Company company = resolveCompany(tenantId, order.getOriginCompanyId());

        int effectiveSeries = series > 0 ? series : 1;
        long invoiceNumber = fiscalRepository.getNextInvoiceNumber(tenantId, company.getId(), "55", effectiveSeries);

        String ufCode = extractUfCode(company);
        String accessKey = accessKeyGenerator.generateKey(
                ufCode,
                LocalDate.now(),
                company.getCnpj(),
                "55",
                effectiveSeries,
                invoiceNumber,
                1 // Emissão Normal
        );

        // Mapeamento de itens para NF-e
        List<NfeXmlBuilder.FiscalItem> fiscalItems = new ArrayList<>();
        int itemIndex = 1;
        for (OrderItem oi : order.getItems()) {
            fiscalItems.add(new NfeXmlBuilder.FiscalItem(
                    itemIndex++,
                    oi.getSkuCode(),
                    "SEM GTIN",
                    oi.getProductTitle(),
                    oi.getNcm() != null ? oi.getNcm() : "49019900", // NCM de Livro
                    oi.getCfop() != null ? oi.getCfop() : "5102",
                    "UN",
                    oi.getQuantity(),
                    oi.getUnitPrice(),
                    oi.getTotal(),
                    oi.getDiscount()
            ));
        }

        // Partes: Emitente e Destinatário
        NfeXmlBuilder.FiscalParty emitter = mapEmitterParty(company);
        NfeXmlBuilder.FiscalParty recipient = new NfeXmlBuilder.FiscalParty(
                order.getCustomerName(),
                order.getCustomerDocument(),
                "ISENTO",
                order.getShippingAddress() != null ? order.getShippingAddress().street() : "Rua Principal",
                order.getShippingAddress() != null ? order.getShippingAddress().number() : "100",
                order.getShippingAddress() != null ? order.getShippingAddress().complement() : null,
                order.getShippingAddress() != null ? order.getShippingAddress().neighborhood() : "Centro",
                order.getShippingAddress() != null ? order.getShippingAddress().city() : "São Paulo",
                "3550308",
                order.getShippingAddress() != null ? order.getShippingAddress().state() : "SP",
                order.getShippingAddress() != null ? order.getShippingAddress().zipCode() : "01310100"
        );

        String natureOfOp = "VENDA DE MERCADORIAS E-COMMERCE";
        String nfeXml = nfeXmlBuilder.buildNfeXml(
                accessKey,
                "55",
                effectiveSeries,
                invoiceNumber,
                natureOfOp,
                1,
                false, // E-commerce = não presencial
                emitter,
                recipient,
                fiscalItems,
                order.getShippingAmount(),
                order.getDiscount(),
                order.getTotalAmount(),
                "17" // PIX / Cartão
        );

        // Transmissão para SEFAZ
        SefazAuthResult authResult = sefazGateway.authorizeInvoice(nfeXml, accessKey, "55", company.getAddress() != null ? company.getAddress().state() : "SP");

        FiscalInvoice invoice = existingOpt.orElseGet(() -> FiscalInvoice.createForOrder(
                tenantId,
                company.getId(),
                orderId,
                effectiveSeries,
                invoiceNumber,
                accessKey,
                order.getTotalAmount(),
                order.getDiscount(),
                natureOfOp
        ));
        invoice.setSeries(effectiveSeries);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setAccessKey(accessKey);

        if (authResult.authorized()) {
            invoice.authorize(authResult.protocolNumber(), authResult.rawXml());
        } else if (authResult.contingency()) {
            invoice.markAsContingency(authResult.rawXml(), authResult.statusMessage());
        } else {
            invoice.reject(authResult.statusMessage());
        }

        FiscalInvoice saved = fiscalRepository.save(invoice);
        metricsService.recordInvoiceEmitted("55", saved.getStatus().name());

        auditUseCase.recordEvent(
                null,
                "system@livraria.com",
                "SYSTEM",
                "127.0.0.1",
                "FISCAL_NFE_EMITTED",
                "FISCAL_INVOICE",
                (saved.getId() != null ? saved.getId() : invoice.getId()).toString(),
                Map.of("orderId", orderId.toString(), "accessKey", accessKey, "status", saved.getStatus().name())
        );

        log.info("NF-e Modelo 55 emitida para o Pedido {}: Chave={}, Status={}", order.getOrderNumber(), accessKey, saved.getStatus());
        return saved;
    }

    @Override
    @Transactional
    public FiscalInvoice emitNfceForSale(UUID saleId, int series) {
        PosSale sale = posSaleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Venda PDV não encontrada: " + saleId));

        if (sale.getStatus() == SaleStatus.CANCELADA) {
            throw new BusinessException("Não é permitido emitir NFC-e para vendas canceladas.");
        }

        Optional<FiscalInvoice> existingOpt = fiscalRepository.findBySaleId(saleId);
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == FiscalStatus.AUTORIZADA) {
            return existingOpt.get();
        }

        UUID tenantId = sale.getTenantId();
        Company company = resolveCompany(tenantId, sale.getCompanyId());

        int effectiveSeries = series > 0 ? series : 1;
        long invoiceNumber = fiscalRepository.getNextInvoiceNumber(tenantId, company.getId(), "65", effectiveSeries);

        String ufCode = extractUfCode(company);
        String accessKey = accessKeyGenerator.generateKey(
                ufCode,
                LocalDate.now(),
                company.getCnpj(),
                "65",
                effectiveSeries,
                invoiceNumber,
                1 // Emissão Normal NFC-e
        );

        List<NfeXmlBuilder.FiscalItem> fiscalItems = new ArrayList<>();
        int itemIndex = 1;
        for (PosSaleItem si : sale.getItems()) {
            fiscalItems.add(new NfeXmlBuilder.FiscalItem(
                    itemIndex++,
                    si.getSkuCode(),
                    "SEM GTIN",
                    si.getTitle(),
                    "49019900", // NCM Livros
                    "5102",
                    "UN",
                    si.getQuantity(),
                    si.getUnitPrice(),
                    si.getSubtotal(),
                    si.getDiscount()
            ));
        }

        NfeXmlBuilder.FiscalParty emitter = mapEmitterParty(company);
        NfeXmlBuilder.FiscalParty recipient = null;
        if (sale.getCustomerCpfCnpj() != null && !sale.getCustomerCpfCnpj().isBlank()) {
            recipient = new NfeXmlBuilder.FiscalParty(
                    sale.getCustomerName() != null ? sale.getCustomerName() : "CONSUMIDOR",
                    sale.getCustomerCpfCnpj(),
                    "ISENTO",
                    null, null, null, null, null, null, null, null
            );
        }

        String nfceXml = nfeXmlBuilder.buildNfeXml(
                accessKey,
                "65",
                effectiveSeries,
                invoiceNumber,
                "VENDA A CONSUMIDOR FINAL PDV",
                1,
                true, // PDV = presencial
                emitter,
                recipient,
                fiscalItems,
                null,
                sale.getDiscount(),
                sale.getTotal(),
                "01" // Dinheiro / Balcão
        );

        SefazAuthResult authResult = sefazGateway.authorizeInvoice(nfceXml, accessKey, "65", company.getAddress() != null ? company.getAddress().state() : "SP");

        FiscalInvoice invoice = existingOpt.orElseGet(() -> FiscalInvoice.createForPosSale(
                tenantId,
                company.getId(),
                saleId,
                effectiveSeries,
                invoiceNumber,
                accessKey,
                sale.getTotal(),
                sale.getDiscount()
        ));
        invoice.setSeries(effectiveSeries);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setAccessKey(accessKey);

        if (authResult.authorized()) {
            invoice.authorize(authResult.protocolNumber(), authResult.rawXml());
            sale.setNfceData(accessKey, authResult.rawXml(), NfceStatus.EMITIDA);
        } else if (authResult.contingency()) {
            invoice.markAsContingency(authResult.rawXml(), authResult.statusMessage());
            sale.setNfceData(accessKey, authResult.rawXml(), NfceStatus.CONTINGENCIA_OFFLINE);
        } else {
            invoice.reject(authResult.statusMessage());
        }

        posSaleRepository.save(sale);
        FiscalInvoice saved = fiscalRepository.save(invoice);
        metricsService.recordInvoiceEmitted("65", saved.getStatus().name());

        auditUseCase.recordEvent(
                null,
                "cashier@livraria.com",
                "CASHIER",
                "127.0.0.1",
                "FISCAL_NFCE_EMITTED",
                "FISCAL_INVOICE",
                (saved.getId() != null ? saved.getId() : invoice.getId()).toString(),
                Map.of("saleId", saleId.toString(), "accessKey", accessKey, "status", saved.getStatus().name())
        );

        log.info("NFC-e Modelo 65 emitida para Venda PDV {}: Chave={}, Status={}", sale.getSaleNumber(), accessKey, saved.getStatus());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public FiscalInvoice findById(UUID id) {
        return fiscalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nota fiscal não encontrada: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public FiscalInvoice findByAccessKey(String accessKey) {
        return fiscalRepository.findByAccessKey(accessKey)
                .orElseThrow(() -> new EntityNotFoundException("Nota fiscal não encontrada para a chave de acesso: " + accessKey));
    }

    @Override
    @Transactional(readOnly = true)
    public String getInvoiceXml(UUID id) {
        FiscalInvoice invoice = findById(id);
        if (invoice.getXmlPayload() == null || invoice.getXmlPayload().isBlank()) {
            throw new BusinessException("XML do documento fiscal não disponível para download.");
        }
        return invoice.getXmlPayload();
    }

    @Override
    @Transactional(readOnly = true)
    public String generateDanfeMirror(UUID id) {
        FiscalInvoice invoice = findById(id);
        Company company = companyRepository.findById(invoice.getCompanyId()).orElse(null);
        String companyName = company != null ? company.getName() : "Livraria & E-Commerce Platform";
        String companyCnpj = company != null ? company.getCnpj() : "00.000.000/0001-91";
        return danfeHtmlGenerator.generateHtml(invoice, companyName, companyCnpj);
    }

    @Override
    @Transactional
    public FiscalInvoice cancelInvoice(UUID id, String reason) {
        FiscalInvoice invoice = findById(id);
        Company company = companyRepository.findById(invoice.getCompanyId()).orElse(null);
        String uf = (company != null && company.getAddress() != null) ? company.getAddress().state() : "SP";

        FiscalCancellationResult result = sefazGateway.cancelInvoice(
                invoice.getAccessKey(),
                invoice.getProtocolNumber(),
                reason,
                uf
        );

        if (!result.success()) {
            throw new BusinessException("Rejeição SEFAZ ao cancelar nota fiscal: " + result.statusMessage());
        }

        invoice.cancel(result.protocolNumber(), reason);
        FiscalInvoice saved = fiscalRepository.save(invoice);
        metricsService.incrementInvoicesCanceled();

        auditUseCase.recordEvent(
                null,
                "operator@livraria.com",
                "OPERATOR",
                "127.0.0.1",
                "FISCAL_INVOICE_CANCELLED",
                "FISCAL_INVOICE",
                saved.getId().toString(),
                Map.of("accessKey", saved.getAccessKey(), "reason", reason, "protocol", result.protocolNumber())
        );

        return saved;
    }

    @Override
    public SefazStatusResult consultSefazStatus(String uf) {
        return sefazGateway.checkServiceStatus(uf != null ? uf : "SP", null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiscalInvoice> listInvoices(int page, int size) {
        UUID tenantId = TenantContext.getTenantId()
                .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return fiscalRepository.findAllByTenant(tenantId, page, size);
    }

    private Company resolveCompany(UUID tenantId, UUID originCompanyId) {
        if (originCompanyId != null) {
            Optional<Company> companyOpt = companyRepository.findById(originCompanyId);
            if (companyOpt.isPresent()) {
                return companyOpt.get();
            }
        }
        List<Company> companies = companyRepository.findAllByTenantId(tenantId);
        return companies.stream()
                .filter(Company::isHeadquarters)
                .findFirst()
                .or(() -> companies.stream().findFirst())
                .orElseGet(() -> Company.create(
                        tenantId,
                        "MATRIZ",
                        "Livraria Matriz Ltda",
                        "Livraria Central",
                        "12.345.678/0001-90",
                        "123456789",
                        null,
                        null,
                        true,
                        new Address("Av. Paulista", "1000", "Andar 5", "Bela Vista", "São Paulo", "SP", "01310-100")
                ));
    }

    private String extractUfCode(Company company) {
        if (company.getAddress() != null && company.getAddress().state() != null) {
            String state = company.getAddress().state().toUpperCase();
            return switch (state) {
                case "SP" -> "35";
                case "RJ" -> "33";
                case "MG" -> "31";
                case "RS" -> "43";
                case "PR" -> "41";
                case "SC" -> "42";
                case "BA" -> "29";
                case "PE" -> "26";
                default -> "35";
            };
        }
        return "35";
    }

    private NfeXmlBuilder.FiscalParty mapEmitterParty(Company company) {
        Address addr = company.getAddress();
        return new NfeXmlBuilder.FiscalParty(
                company.getName(),
                company.getCnpj(),
                company.getStateRegistration() != null ? company.getStateRegistration() : "ISENTO",
                addr != null ? addr.street() : "Av. Paulista",
                addr != null ? addr.number() : "1000",
                addr != null ? addr.complement() : null,
                addr != null ? addr.neighborhood() : "Bela Vista",
                addr != null ? addr.city() : "São Paulo",
                "3550308",
                addr != null ? addr.state() : "SP",
                addr != null ? addr.zipCode() : "01310100"
        );
    }
}
