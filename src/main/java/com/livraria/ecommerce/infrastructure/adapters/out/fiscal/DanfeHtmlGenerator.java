package com.livraria.ecommerce.infrastructure.adapters.out.fiscal;

import com.livraria.ecommerce.domain.model.fiscal.FiscalInvoice;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class DanfeHtmlGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Sao_Paulo"));

    public String generateHtml(FiscalInvoice invoice, String companyName, String companyCnpj) {
        String authDate = invoice.getAuthorizedAt() != null ? FORMATTER.format(invoice.getAuthorizedAt()) : "N/A";
        String accessKeyFormatted = formatAccessKey(invoice.getAccessKey());

        return "<!DOCTYPE html>\n" +
                "<html lang=\"pt-BR\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\"/>\n" +
                "  <title>DANFE - Documento Auxiliar da Nota Fiscal Eletrônica</title>\n" +
                "  <style>\n" +
                "    body { font-family: 'Helvetica Neue', Arial, sans-serif; margin: 20px; color: #1e293b; font-size: 13px; }\n" +
                "    .danfe-box { border: 2px solid #0f172a; padding: 15px; border-radius: 6px; max-width: 900px; margin: 0 auto; }\n" +
                "    .header { display: flex; justify-content: space-between; border-bottom: 2px solid #0f172a; padding-bottom: 12px; margin-bottom: 12px; }\n" +
                "    .emitter-title { font-size: 18px; font-weight: bold; color: #0284c7; }\n" +
                "    .key-box { background: #f8fafc; border: 1px dashed #64748b; padding: 10px; font-family: monospace; font-size: 14px; text-align: center; margin: 10px 0; font-weight: bold; letter-spacing: 1px; }\n" +
                "    .grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 12px; }\n" +
                "    .card { background: #f1f5f9; padding: 8px 12px; border-radius: 4px; }\n" +
                "    .card-label { font-size: 10px; color: #64748b; text-transform: uppercase; font-weight: bold; }\n" +
                "    .card-val { font-size: 13px; font-weight: 600; margin-top: 2px; }\n" +
                "    .status-badge { display: inline-block; padding: 4px 10px; border-radius: 9999px; font-weight: bold; font-size: 11px; }\n" +
                "    .status-AUTORIZADA { background: #dcfce7; color: #166534; }\n" +
                "    .status-CONTINGENCIA { background: #fef9c3; color: #854d0e; }\n" +
                "    .status-CANCELADA { background: #fee2e2; color: #991b1b; }\n" +
                "    .legal-note { font-size: 11px; color: #475569; margin-top: 15px; border-top: 1px solid #cbd5e1; padding-top: 10px; font-style: italic; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"danfe-box\">\n" +
                "    <div class=\"header\">\n" +
                "      <div>\n" +
                "        <div class=\"emitter-title\">" + escape(companyName != null ? companyName : "Livraria & E-commerce Multitenant") + "</div>\n" +
                "        <div>CNPJ Emitente: " + escape(companyCnpj != null ? companyCnpj : "00.000.000/0001-91") + "</div>\n" +
                "        <div>Natureza da Operação: " + escape(invoice.getNatureOfOperation()) + "</div>\n" +
                "      </div>\n" +
                "      <div style=\"text-align: right;\">\n" +
                "        <div style=\"font-size: 16px; font-weight: bold;\">DANFE " + (invoice.getModel() != null ? invoice.getModel().name() : "NF-e") + "</div>\n" +
                "        <div>Série: <strong>" + invoice.getSeries() + "</strong> | Número: <strong>" + invoice.getInvoiceNumber() + "</strong></div>\n" +
                "        <div style=\"margin-top: 4px;\"><span class=\"status-badge status-" + invoice.getStatus().name() + "\">" + invoice.getStatus().name() + "</span></div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div>CHAVE DE ACESSO PARA CONSULTA NO PORTAL NACIONAL DA SEFAZ:</div>\n" +
                "    <div class=\"key-box\">" + accessKeyFormatted + "</div>\n" +
                "    <div class=\"grid\">\n" +
                "      <div class=\"card\"><div class=\"card-label\">Protocolo de Autorização</div><div class=\"card-val\">" + escape(invoice.getProtocolNumber() != null ? invoice.getProtocolNumber() : "AGUARDANDO SEFAZ") + "</div></div>\n" +
                "      <div class=\"card\"><div class=\"card-label\">Data de Autorização</div><div class=\"card-val\">" + authDate + "</div></div>\n" +
                "      <div class=\"card\"><div class=\"card-label\">Valor Total dos Produtos</div><div class=\"card-val\">R$ " + invoice.getTotalAmount() + "</div></div>\n" +
                "      <div class=\"card\"><div class=\"card-label\">Valor Total da NF-e</div><div class=\"card-val\">R$ " + invoice.getTotalAmount().subtract(invoice.getTotalDiscount()) + "</div></div>\n" +
                "    </div>\n" +
                "    <div class=\"legal-note\">\n" +
                "      DOCUMENTO EMITIDO COM IMUNIDADE TRIBUTÁRIA CONFORME ART. 150, VI, 'd' DA CONSTITUIÇÃO FEDERAL DE 1988 (LIVROS E PERIÓDICOS).\n" +
                "      Base de Cálculo ICMS: R$ 0,00 | ICMS: R$ 0,00 | PIS: R$ 0,00 | COFINS: R$ 0,00.\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    private String formatAccessKey(String key) {
        if (key == null || key.length() != 44) return key != null ? key : "";
        StringBuilder sb = new StringBuilder(55);
        for (int i = 0; i < 44; i += 4) {
            if (i > 0) sb.append(' ');
            sb.append(key, i, Math.min(i + 4, 44));
        }
        return sb.toString();
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
