package com.livraria.ecommerce.domain;

import com.livraria.ecommerce.domain.model.pos.NfceGenerator;
import com.livraria.ecommerce.domain.model.pos.NfceStatus;
import com.livraria.ecommerce.domain.model.pos.PaymentMethod;
import com.livraria.ecommerce.domain.model.pos.PosSale;
import com.livraria.ecommerce.domain.model.pos.PosSaleItem;
import com.livraria.ecommerce.domain.model.pos.PosSalePayment;
import com.livraria.ecommerce.domain.model.pos.SaleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - NfceGenerator (Geração de Chave de Acesso de 44 Dígitos e XML de NFC-e)")
class NfceGeneratorTest {

    @Test
    void shouldGenerateValid44DigitAccessKey() {
        String cnpj = "12345678000195";
        String accessKey = NfceGenerator.generateAccessKey(cnpj, 1, 1050L, false);

        assertNotNull(accessKey);
        assertEquals(44, accessKey.length());
        assertTrue(accessKey.matches("\\d{44}"));
        // Modelo 65 deve estar na posição correspondente
        assertTrue(accessKey.contains("65"));
    }

    @Test
    void shouldBuildSampleXmlWithValidElements() {
        PosSaleItem item = new PosSaleItem(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "LIV-01", "Livro",
                1, new BigDecimal("49.90"), BigDecimal.ZERO, new BigDecimal("49.90")
        );
        PosSalePayment p = new PosSalePayment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PaymentMethod.PIX, new BigDecimal("49.90"));

        PosSale sale = new PosSale(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "VD-1234", null,
                "Cliente", "11122233344", BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, SaleStatus.CONCLUIDA,
                null, null, NfceStatus.EMITIDA,
                List.of(item), List.of(p), UUID.randomUUID(), Instant.now(), Instant.now()
        );

        String accessKey = "35261112345678000195650010000010501123456789";
        String xml = NfceGenerator.buildSampleNfceXml(accessKey, sale, "Livraria Cultura", "12345678000195");

        assertNotNull(xml);
        assertTrue(xml.startsWith("<?xml"));
        assertTrue(xml.contains("<mod>65</mod>"));
        assertTrue(xml.contains("<vNF>49.90</vNF>"));
        assertTrue(xml.contains("Livraria Cultura"));
    }
}
