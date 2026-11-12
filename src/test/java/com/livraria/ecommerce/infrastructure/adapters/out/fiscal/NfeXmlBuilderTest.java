package com.livraria.ecommerce.infrastructure.adapters.out.fiscal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - NfeXmlBuilder (Montagem e Schema do XML de NF-e e NFC-e)")
class NfeXmlBuilderTest {

    private NfeXmlBuilder xmlBuilder;

    @BeforeEach
    void setUp() {
        xmlBuilder = new NfeXmlBuilder();
    }

    @Test
    @DisplayName("Deve gerar XML de NF-e v4.00 com imunidade de livros e tags canônicas")
    void shouldBuildValidNfeXml() {
        String accessKey = "35261112345678000190550010000001001123456781";

        NfeXmlBuilder.FiscalParty emitter = new NfeXmlBuilder.FiscalParty(
                "Livraria Matriz Ltda",
                "12345678000190",
                "123456789",
                "Av. Paulista", "1000", "Andar 5", "Bela Vista", "São Paulo", "3550308", "SP", "01310100"
        );

        NfeXmlBuilder.FiscalParty recipient = new NfeXmlBuilder.FiscalParty(
                "Maria Oliveira",
                "12345678901",
                "ISENTO",
                "Rua das Flores", "123", null, "Jardins", "São Paulo", "3550308", "SP", "01400000"
        );

        List<NfeXmlBuilder.FiscalItem> items = List.of(
                new NfeXmlBuilder.FiscalItem(
                        1,
                        "LIV-DOM-CASMURRO",
                        "9788535914849",
                        "Dom Casmurro - Machado de Assis",
                        "49019900",
                        "5102",
                        "UN",
                        2,
                        new BigDecimal("49.90"),
                        new BigDecimal("99.80"),
                        BigDecimal.ZERO
                )
        );

        String xml = xmlBuilder.buildNfeXml(
                accessKey,
                "55",
                1,
                100L,
                "VENDA DE MERCADORIAS",
                1,
                false,
                emitter,
                recipient,
                items,
                new BigDecimal("15.00"),
                BigDecimal.ZERO,
                new BigDecimal("114.80"),
                "17"
        );

        assertNotNull(xml);
        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<infNFe Id=\"NFe" + accessKey + "\" versao=\"4.00\">"));
        assertTrue(xml.contains("<mod>55</mod>"));
        assertTrue(xml.contains("<serie>1</serie>"));
        assertTrue(xml.contains("<nNF>100</nNF>"));
        assertTrue(xml.contains("<xProd>Dom Casmurro - Machado de Assis</xProd>"));
        assertTrue(xml.contains("<NCM>49019900</NCM>"));
        assertTrue(xml.contains("<CFOP>5102</CFOP>"));

        // Imunidade Tributária de Livros (Art. 150, VI, 'd')
        assertTrue(xml.contains("<CSOSN>400</CSOSN>"));
        assertTrue(xml.contains("<CST>08</CST>"));
        assertTrue(xml.contains("Documento emitido com imunidade tributária nos termos do art. 150, VI, 'd' da CF/88"));

        // Assinatura digital simulada
        assertTrue(xml.contains("<Signature xmlns=\"http://www.w3.org/2000/09/xmldsig#\">"));
    }

    @Test
    @DisplayName("Deve envelopar XML da NF-e com o protocolo de autorização SEFAZ (nfeProc)")
    void shouldWrapWithProtocol() {
        String baseXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<NFe><infNFe></infNFe></NFe>";
        String protocolNumber = "135260001234567";
        String accessKey = "35261112345678000190550010000001001123456781";
        String digestVal = "k7H1e2u8z9==";

        String processedXml = xmlBuilder.wrapWithProtocol(baseXml, protocolNumber, digestVal, accessKey);

        assertNotNull(processedXml);
        assertTrue(processedXml.contains("<nfeProc xmlns=\"http://www.portalfiscal.inf.br/nfe\" versao=\"4.00\">"));
        assertTrue(processedXml.contains("<protNFe versao=\"4.00\">"));
        assertTrue(processedXml.contains("<nProt>" + protocolNumber + "</nProt>"));
        assertTrue(processedXml.contains("<chNFe>" + accessKey + "</chNFe>"));
        assertTrue(processedXml.contains("<cStat>100</cStat>"));
        assertTrue(processedXml.contains("<xMotivo>Autorizado o uso da NF-e</xMotivo>"));
    }
}

