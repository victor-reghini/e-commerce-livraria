package com.livraria.ecommerce.infrastructure.adapters.out.fiscal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Unitários - SefazAccessKeyGenerator (Chave de Acesso Canônica 44 Dígitos & Módulo 11)")
class SefazAccessKeyGeneratorTest {

    private SefazAccessKeyGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SefazAccessKeyGenerator();
    }

    @Test
    @DisplayName("Deve gerar chave de acesso válida com exatamente 44 dígitos numéricos")
    void shouldGenerateValid44DigitKey() {
        String key = generator.generateKey(
                "35",
                LocalDate.of(2026, 11, 12),
                "12.345.678/0001-90",
                "55",
                1,
                100L,
                1
        );

        assertNotNull(key);
        assertEquals(44, key.length(), "A chave da SEFAZ deve conter exatamente 44 dígitos");
        assertTrue(key.matches("\\d{44}"), "A chave deve conter apenas dígitos numéricos");

        // Prefixos canônicos
        assertEquals("35", key.substring(0, 2), "cUF deve ser 35 (SP)");
        assertEquals("2611", key.substring(2, 6), "AAMM deve ser 2611");
        assertEquals("12345678000190", key.substring(6, 20), "CNPJ formatado com 14 dígitos");
        assertEquals("55", key.substring(20, 22), "Modelo 55");
        assertEquals("001", key.substring(22, 25), "Série 001");
        assertEquals("000000100", key.substring(25, 34), "Número 100 com zeros à esquerda");
        assertEquals("1", key.substring(34, 35), "Tipo de emissão 1 (Normal)");

        // Verificação do dígito verificador módulo 11
        assertTrue(SefazAccessKeyGenerator.isValidKey(key));
    }

    @Test
    @DisplayName("Deve calcular dígito verificador pelo Módulo 11 corretamente")
    void shouldCalculateModulo11Correctly() {
        // Exemplo de chave parcial com 43 dígitos
        String partialKey = "3526111234567800019055001000000100112345678";
        assertEquals(43, partialKey.length());

        int dv = SefazAccessKeyGenerator.calculateModulo11(partialKey);
        assertTrue(dv >= 0 && dv <= 9, "DV deve ser entre 0 e 9");

        String fullKey = partialKey + dv;
        assertTrue(SefazAccessKeyGenerator.isValidKey(fullKey));
    }

    @Test
    @DisplayName("Deve invalidar chave com dígito verificador incorreto ou tamanho inválido")
    void shouldRejectInvalidKeys() {
        assertFalse(SefazAccessKeyGenerator.isValidKey(null));
        assertFalse(SefazAccessKeyGenerator.isValidKey("123"));
        assertFalse(SefazAccessKeyGenerator.isValidKey("3526111234567800019055001000000100112345678X"));

        // Chave com 44 dígitos mas DV corrompido
        String validKey = generator.generateKey("35", LocalDate.now(), "12345678000190", "55", 1, 1L, 1);
        int lastDigit = Character.getNumericValue(validKey.charAt(43));
        int wrongDigit = (lastDigit + 1) % 10;
        String corruptedKey = validKey.substring(0, 43) + wrongDigit;

        assertFalse(SefazAccessKeyGenerator.isValidKey(corruptedKey));
    }
}

