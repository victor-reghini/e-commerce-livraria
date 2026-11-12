package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.fiscal.FiscalDto;
import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import com.livraria.ecommerce.domain.model.fiscal.SefazStatusResult;
import com.livraria.ecommerce.domain.port.in.FiscalUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fiscal")
@Tag(name = "Homologação Fiscal SEFAZ", description = "Emissão, consulta, homologação e cancelamento de NF-e (Modelo 55) e NFC-e (Modelo 65)")
public class FiscalController {

    private final FiscalUseCase fiscalUseCase;

    public FiscalController(FiscalUseCase fiscalUseCase) {
        this.fiscalUseCase = fiscalUseCase;
    }

    @PostMapping("/invoices/nfe/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('FISCAL_EMIT_NFE', 'ORDER_WRITE', 'ADMIN', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Emitir NF-e Modelo 55 para Pedido E-Commerce/Corporativo",
            description = "Gera chave de acesso de 44 dígitos com dígito verificador Módulo 11, monta o XML canônico com imunidade tributária de livros e transmite para autorização SEFAZ.")
    public ResponseEntity<FiscalDto.FiscalInvoiceResponse> emitNfeForOrder(
            @PathVariable UUID orderId,
            @RequestBody(required = false) FiscalDto.EmitNfeRequest request) {
        int series = (request != null && request.series() != null) ? request.series() : 1;
        FiscalInvoice invoice = fiscalUseCase.emitNfeForOrder(orderId, series);
        return ResponseEntity.status(HttpStatus.CREATED).body(FiscalDto.FiscalInvoiceResponse.fromDomain(invoice));
    }

    @PostMapping("/invoices/nfce/sale/{saleId}")
    @PreAuthorize("hasAnyAuthority('FISCAL_EMIT_NFCE', 'PDV_SELL', 'ADMIN', 'ROLE_TENANT_ADMIN')")
    @Operation(summary = "Emitir NFC-e Modelo 65 para Venda PDV Balcão/Feira",
            description = "Gera chave de 44 dígitos e autoriza NFC-e perante a SEFAZ ou ativa contingência offline caso SEFAZ esteja inoperante.")
    public ResponseEntity<FiscalDto.FiscalInvoiceResponse> emitNfceForSale(
            @PathVariable UUID saleId,
            @RequestBody(required = false) FiscalDto.EmitNfceRequest request) {
        int series = (request != null && request.series() != null) ? request.series() : 1;
        FiscalInvoice invoice = fiscalUseCase.emitNfceForSale(saleId, series);
        return ResponseEntity.status(HttpStatus.CREATED).body(FiscalDto.FiscalInvoiceResponse.fromDomain(invoice));
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Obter detalhes de uma Nota Fiscal pelo ID interno")
    public ResponseEntity<FiscalDto.FiscalInvoiceResponse> getById(@PathVariable UUID id) {
        FiscalInvoice invoice = fiscalUseCase.findById(id);
        return ResponseEntity.ok(FiscalDto.FiscalInvoiceResponse.fromDomain(invoice));
    }

    @GetMapping("/invoices/key/{accessKey}")
    @Operation(summary = "Consultar Nota Fiscal pela Chave de Acesso de 44 dígitos")
    public ResponseEntity<FiscalDto.FiscalInvoiceResponse> getByAccessKey(@PathVariable String accessKey) {
        FiscalInvoice invoice = fiscalUseCase.findByAccessKey(accessKey);
        return ResponseEntity.ok(FiscalDto.FiscalInvoiceResponse.fromDomain(invoice));
    }

    @GetMapping(value = "/invoices/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Download do XML da Nota Fiscal Eletrônica com Protocolo de Autorização SEFAZ")
    public ResponseEntity<String> getInvoiceXml(@PathVariable UUID id) {
        String xml = fiscalUseCase.getInvoiceXml(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"nfe-" + id + ".xml\"")
                .body(xml);
    }

    @GetMapping(value = "/invoices/{id}/danfe", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Visualização e Espelho do DANFE em formato HTML responsivo para impressão")
    public ResponseEntity<String> getDanfeMirror(@PathVariable UUID id) {
        String html = fiscalUseCase.generateDanfeMirror(id);
        return ResponseEntity.ok(html);
    }

    @PostMapping("/invoices/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('FISCAL_CANCEL', 'ADMIN')")
    @Operation(summary = "Cancelar Nota Fiscal perante a SEFAZ com Registro de Evento (cStat 135)")
    public ResponseEntity<FiscalDto.FiscalInvoiceResponse> cancelInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody FiscalDto.CancelInvoiceRequest request) {
        FiscalInvoice canceled = fiscalUseCase.cancelInvoice(id, request.reason());
        return ResponseEntity.ok(FiscalDto.FiscalInvoiceResponse.fromDomain(canceled));
    }

    @GetMapping("/status-sefaz")
    @Operation(summary = "Consultar Status Operacional do WebService da SEFAZ (consStatServ - 107)")
    public ResponseEntity<FiscalDto.SefazStatusResponse> consultSefazStatus(
            @RequestParam(defaultValue = "SP") String uf) {
        SefazStatusResult status = fiscalUseCase.consultSefazStatus(uf);
        return ResponseEntity.ok(FiscalDto.SefazStatusResponse.fromDomain(status));
    }

    @GetMapping("/invoices")
    @Operation(summary = "Listar Notas Fiscais emitidas pelo Tenant com paginação")
    public ResponseEntity<List<FiscalDto.FiscalInvoiceResponse>> listInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<FiscalInvoice> invoices = fiscalUseCase.listInvoices(page, size);
        List<FiscalDto.FiscalInvoiceResponse> responses = invoices.stream()
                .map(FiscalDto.FiscalInvoiceResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(responses);
    }
}
