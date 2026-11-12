package com.livraria.ecommerce.infrastructure.adapters.out.fiscal;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NfeXmlBuilder {

    public record FiscalItem(
            int itemIndex,
            String productCode,
            String ean,
            String description,
            String ncm,
            String cfop,
            String unit,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal,
            BigDecimal discount
    ) {}

    public record FiscalParty(
            String name,
            String document, // CPF or CNPJ
            String stateRegistration,
            String street,
            String number,
            String complement,
            String neighborhood,
            String city,
            String cityCode,
            String state,
            String zipCode
    ) {}

    public String buildNfeXml(
            String accessKey,
            String model, // "55" or "65"
            int series,
            long invoiceNumber,
            String natureOfOperation,
            int emissionType,
            boolean isPresential,
            FiscalParty emitter,
            FiscalParty recipient,
            List<FiscalItem> items,
            BigDecimal shippingAmount,
            BigDecimal totalDiscount,
            BigDecimal totalAmount,
            String paymentMethodCode
    ) {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">\n");
        xml.append("  <infNFe Id=\"NFe").append(accessKey).append("\" versao=\"4.00\">\n");

        // 1. Identificação da NF-e (ide)
        String ufCode = accessKey.substring(0, 2);
        String cnf = accessKey.substring(35, 43);
        int cDV = Character.getNumericValue(accessKey.charAt(43));
        String dhEmi = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        xml.append("    <ide>\n");
        xml.append("      <cUF>").append(ufCode).append("</cUF>\n");
        xml.append("      <cNF>").append(cnf).append("</cNF>\n");
        xml.append("      <natOp>").append(escapeXml(natureOfOperation)).append("</natOp>\n");
        xml.append("      <mod>").append(model).append("</mod>\n");
        xml.append("      <serie>").append(series).append("</serie>\n");
        xml.append("      <nNF>").append(invoiceNumber).append("</nNF>\n");
        xml.append("      <dhEmi>").append(dhEmi).append("</dhEmi>\n");
        xml.append("      <tpNF>1</tpNF>\n"); // 1 = Saída
        xml.append("      <idDest>1</idDest>\n"); // 1 = Operação interna
        xml.append("      <cMunFG>").append(emitter.cityCode() != null ? emitter.cityCode() : "3550308").append("</cMunFG>\n");
        xml.append("      <tpImp>").append("55".equals(model) ? "1" : "4").append("</tpImp>\n"); // 1 = Retrato, 4 = NFC-e
        xml.append("      <tpEmis>").append(emissionType).append("</tpEmis>\n");
        xml.append("      <cDV>").append(cDV).append("</cDV>\n");
        xml.append("      <tpAmb>2</tpAmb>\n"); // 2 = Homologação
        xml.append("      <finNFe>1</finNFe>\n"); // 1 = Normal
        xml.append("      <indFinal>1</indFinal>\n"); // 1 = Consumidor final
        xml.append("      <indPres>").append(isPresential ? "1" : "2").append("</indPres>\n"); // 1 = Presencial, 2 = Internet
        xml.append("      <procEmi>0</procEmi>\n"); // Aplicativo do contribuinte
        xml.append("      <verProc>1.0.0-PROD</verProc>\n");
        xml.append("    </ide>\n");

        // 2. Emitente (emit)
        String emitDoc = emitter.document() != null ? emitter.document().replaceAll("\\D", "") : "00000000000191";
        xml.append("    <emit>\n");
        if (emitDoc.length() > 11) {
            xml.append("      <CNPJ>").append(emitDoc).append("</CNPJ>\n");
        } else {
            xml.append("      <CPF>").append(emitDoc).append("</CPF>\n");
        }
        xml.append("      <xNome>").append(escapeXml(emitter.name())).append("</xNome>\n");
        xml.append("      <enderEmit>\n");
        xml.append("        <xLgr>").append(escapeXml(emitter.street())).append("</xLgr>\n");
        xml.append("        <nro>").append(escapeXml(emitter.number())).append("</nro>\n");
        if (emitter.complement() != null && !emitter.complement().isBlank()) {
            xml.append("        <xCpl>").append(escapeXml(emitter.complement())).append("</xCpl>\n");
        }
        xml.append("        <xBairro>").append(escapeXml(emitter.neighborhood())).append("</xBairro>\n");
        xml.append("        <cMun>").append(emitter.cityCode() != null ? emitter.cityCode() : "3550308").append("</cMun>\n");
        xml.append("        <xMun>").append(escapeXml(emitter.city())).append("</xMun>\n");
        xml.append("        <UF>").append(emitter.state()).append("</UF>\n");
        xml.append("        <CEP>").append(emitter.zipCode() != null ? emitter.zipCode().replaceAll("\\D", "") : "01310100").append("</CEP>\n");
        xml.append("        <cPais>1058</cPais>\n");
        xml.append("        <xPais>BRASIL</xPais>\n");
        xml.append("      </enderEmit>\n");
        xml.append("      <IE>").append(emitter.stateRegistration() != null ? emitter.stateRegistration() : "ISENTO").append("</IE>\n");
        xml.append("      <CRT>1</CRT>\n"); // 1 = Simples Nacional
        xml.append("    </emit>\n");

        // 3. Destinatário (dest)
        if (recipient != null && recipient.document() != null && !recipient.document().isBlank()) {
            String destDoc = recipient.document().replaceAll("\\D", "");
            xml.append("    <dest>\n");
            if (destDoc.length() > 11) {
                xml.append("      <CNPJ>").append(destDoc).append("</CNPJ>\n");
            } else {
                xml.append("      <CPF>").append(destDoc).append("</CPF>\n");
            }
            xml.append("      <xNome>").append(escapeXml(recipient.name() != null ? recipient.name() : "CONSUMIDOR FINAL")).append("</xNome>\n");
            if (recipient.street() != null && !recipient.street().isBlank()) {
                xml.append("      <enderDest>\n");
                xml.append("        <xLgr>").append(escapeXml(recipient.street())).append("</xLgr>\n");
                xml.append("        <nro>").append(escapeXml(recipient.number() != null ? recipient.number() : "S/N")).append("</nro>\n");
                xml.append("        <xBairro>").append(escapeXml(recipient.neighborhood() != null ? recipient.neighborhood() : "CENTRO")).append("</xBairro>\n");
                xml.append("        <cMun>").append(recipient.cityCode() != null ? recipient.cityCode() : "3550308").append("</cMun>\n");
                xml.append("        <xMun>").append(escapeXml(recipient.city() != null ? recipient.city() : "SAO PAULO")).append("</xMun>\n");
                xml.append("        <UF>").append(recipient.state() != null ? recipient.state() : "SP").append("</UF>\n");
                xml.append("        <CEP>").append(recipient.zipCode() != null ? recipient.zipCode().replaceAll("\\D", "") : "01001000").append("</CEP>\n");
                xml.append("        <cPais>1058</cPais>\n");
                xml.append("        <xPais>BRASIL</xPais>\n");
                xml.append("      </enderDest>\n");
            }
            xml.append("      <indIEDest>9</indIEDest>\n"); // 9 = Não Contribuinte
            xml.append("    </dest>\n");
        }

        // 4. Detalhamento de Produtos e Serviços (det)
        BigDecimal totalProd = BigDecimal.ZERO;
        int itemIndex = 1;
        for (FiscalItem item : items) {
            BigDecimal itemTotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())).setScale(2, RoundingMode.HALF_UP);
            totalProd = totalProd.add(itemTotal);

            xml.append("    <det nItem=\"").append(itemIndex++).append("\">\n");
            xml.append("      <prod>\n");
            xml.append("        <cProd>").append(escapeXml(item.productCode())).append("</cProd>\n");
            xml.append("        <cEAN>").append(item.ean() != null && !item.ean().isBlank() ? item.ean() : "SEM GTIN").append("</cEAN>\n");
            xml.append("        <xProd>").append(escapeXml(item.description())).append("</xProd>\n");
            xml.append("        <NCM>").append(item.ncm() != null ? item.ncm().replaceAll("\\D", "") : "49019900").append("</NCM>\n"); // NCM 49019900 = Livros
            xml.append("        <CFOP>").append(item.cfop() != null ? item.cfop() : ("55".equals(model) ? "5102" : "5102")).append("</CFOP>\n");
            xml.append("        <uCom>").append(item.unit() != null ? item.unit() : "UN").append("</uCom>\n");
            xml.append("        <qCom>").append(String.format("%.4f", (double) item.quantity())).append("</qCom>\n");
            xml.append("        <vUnCom>").append(String.format("%.4f", item.unitPrice().doubleValue())).append("</vUnCom>\n");
            xml.append("        <vProd>").append(String.format("%.2f", itemTotal.doubleValue())).append("</vProd>\n");
            xml.append("        <cEANTrib>").append(item.ean() != null && !item.ean().isBlank() ? item.ean() : "SEM GTIN").append("</cEANTrib>\n");
            xml.append("        <uTrib>").append(item.unit() != null ? item.unit() : "UN").append("</uTrib>\n");
            xml.append("        <qTrib>").append(String.format("%.4f", (double) item.quantity())).append("</qTrib>\n");
            xml.append("        <vUnTrib>").append(String.format("%.4f", item.unitPrice().doubleValue())).append("</vUnTrib>\n");
            if (item.discount() != null && item.discount().compareTo(BigDecimal.ZERO) > 0) {
                xml.append("        <vDesc>").append(String.format("%.2f", item.discount().doubleValue())).append("</vDesc>\n");
            }
            xml.append("        <indTot>1</indTot>\n");
            xml.append("      </prod>\n");

            // Tributação: Imunidade Tributária Constitucional para Livros (Art. 150, VI, 'd' da CF/88)
            xml.append("      <imposto>\n");
            xml.append("        <ICMS>\n");
            xml.append("          <ICMSSN400>\n"); // Simples Nacional - Não tributada (Imunidade)
            xml.append("            <orig>0</orig>\n"); // Nacional
            xml.append("            <CSOSN>400</CSOSN>\n");
            xml.append("          </ICMSSN400>\n");
            xml.append("        </ICMS>\n");
            xml.append("        <PIS>\n");
            xml.append("          <PISNT>\n");
            xml.append("            <CST>08</CST>\n"); // 08 = Operação sem Incidência da Contribuição
            xml.append("          </PISNT>\n");
            xml.append("        </PIS>\n");
            xml.append("        <COFINS>\n");
            xml.append("          <COFINSNT>\n");
            xml.append("            <CST>08</CST>\n"); // 08 = Operação sem Incidência da Contribuição
            xml.append("          </COFINSNT>\n");
            xml.append("        </COFINS>\n");
            xml.append("      </imposto>\n");
            xml.append("    </det>\n");
        }

        // 5. Totalizadores (total)
        BigDecimal frete = shippingAmount != null ? shippingAmount : BigDecimal.ZERO;
        BigDecimal desc = totalDiscount != null ? totalDiscount : BigDecimal.ZERO;
        BigDecimal finalTotal = totalAmount != null ? totalAmount : totalProd.add(frete).subtract(desc);

        xml.append("    <total>\n");
        xml.append("      <ICMSTot>\n");
        xml.append("        <vBC>0.00</vBC>\n");
        xml.append("        <vICMS>0.00</vICMS>\n");
        xml.append("        <vICMSDeson>0.00</vICMSDeson>\n");
        xml.append("        <vFCPUFDest>0.00</vFCPUFDest>\n");
        xml.append("        <vICMSUFDest>0.00</vICMSUFDest>\n");
        xml.append("        <vICMSUFRemet>0.00</vICMSUFRemet>\n");
        xml.append("        <vFCP>0.00</vFCP>\n");
        xml.append("        <vBCST>0.00</vBCST>\n");
        xml.append("        <vST>0.00</vST>\n");
        xml.append("        <vFCPST>0.00</vFCPST>\n");
        xml.append("        <vFCPSTRet>0.00</vFCPSTRet>\n");
        xml.append("        <vProd>").append(String.format("%.2f", totalProd.doubleValue())).append("</vProd>\n");
        xml.append("        <vFrete>").append(String.format("%.2f", frete.doubleValue())).append("</vFrete>\n");
        xml.append("        <vSeg>0.00</vSeg>\n");
        xml.append("        <vDesc>").append(String.format("%.2f", desc.doubleValue())).append("</vDesc>\n");
        xml.append("        <vII>0.00</vII>\n");
        xml.append("        <vIPI>0.00</vIPI>\n");
        xml.append("        <vIPIDevol>0.00</vIPIDevol>\n");
        xml.append("        <vPIS>0.00</vPIS>\n");
        xml.append("        <vCOFINS>0.00</vCOFINS>\n");
        xml.append("        <vOutro>0.00</vOutro>\n");
        xml.append("        <vNF>").append(String.format("%.2f", finalTotal.doubleValue())).append("</vNF>\n");
        xml.append("      </ICMSTot>\n");
        xml.append("    </total>\n");

        // 6. Transporte (transp)
        xml.append("    <transp>\n");
        xml.append("      <modFrete>").append(frete.compareTo(BigDecimal.ZERO) > 0 ? "0" : "9").append("</modFrete>\n");
        xml.append("    </transp>\n");

        // 7. Pagamento (pag)
        String tPag = paymentMethodCode != null ? paymentMethodCode : "01"; // 01 Dinheiro, 03 CC, 15 Boleto, 17 PIX
        xml.append("    <pag>\n");
        xml.append("      <detPag>\n");
        xml.append("        <indPag>0</indPag>\n"); // 0 = Pagamento à Vista
        xml.append("        <tPag>").append(tPag).append("</tPag>\n");
        xml.append("        <vPag>").append(String.format("%.2f", finalTotal.doubleValue())).append("</vPag>\n");
        xml.append("      </detPag>\n");
        xml.append("    </pag>\n");

        // 8. Informações Adicionais (infAdic)
        xml.append("    <infAdic>\n");
        xml.append("      <infCpl>Documento emitido com imunidade tributária nos termos do art. 150, VI, 'd' da CF/88 (Livros). Procon: 151.</infCpl>\n");
        xml.append("    </infAdic>\n");

        xml.append("  </infNFe>\n");

        // 9. Assinatura Digital Simulada A1 (Signature)
        xml.append("  <Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">\n");
        xml.append("    <SignedInfo>\n");
        xml.append("      <CanonicalizationMethod Algorithm=\"http://www.w3.org/TR/2001/REC-xml-c14n-20010315\"/>\n");
        xml.append("      <SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/>\n");
        xml.append("      <Reference URI=\"#NFe").append(accessKey).append("\">\n");
        xml.append("        <DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/>\n");
        xml.append("        <DigestValue>+8yqB3x1cKp/72Fj+qW9E6M1V0M=</DigestValue>\n");
        xml.append("      </Reference>\n");
        xml.append("    </SignedInfo>\n");
        xml.append("    <SignatureValue>MEYCIQDa...</SignatureValue>\n");
        xml.append("  </Signature>\n");
        xml.append("</NFe>");

        return xml.toString();
    }

    public String wrapWithProtocol(String nfeXml, String protocolNumber, String digestValue, String accessKey) {
        String dhRecbto = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">\n" +
                nfeXml.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "") + "\n" +
                "  <protNFe versao=\"4.00\">\n" +
                "    <infProt>\n" +
                "      <tpAmb>2</tpAmb>\n" +
                "      <verAplic>SP_NFE_PL_009_V4</verAplic>\n" +
                "      <chNFe>" + accessKey + "</chNFe>\n" +
                "      <dhRecbto>" + dhRecbto + "</dhRecbto>\n" +
                "      <nProt>" + protocolNumber + "</nProt>\n" +
                "      <digVal>" + (digestValue != null ? digestValue : "+8yqB3x1cKp/72Fj+qW9E6M1V0M=") + "</digVal>\n" +
                "      <cStat>100</cStat>\n" +
                "      <xMotivo>Autorizado o uso da NF-e</xMotivo>\n" +
                "    </infProt>\n" +
                "  </protNFe>\n" +
                "</nfeProc>";
    }

    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}

