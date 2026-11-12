package com.livraria.ecommerce.infrastructure.adapters.out.fiscal;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Component
public class SefazAccessKeyGenerator {

    private final Random random = new Random();

    /**
     * Gera a Chave de Acesso canônica de 44 dígitos da SEFAZ com dígito verificador Módulo 11.
     *
     * Estrutura:
     * - cUF (2 dígitos): Código da UF da empresa emitente (ex: SP = 35)
     * - AAMM (4 dígitos): Ano e mês de emissão (ex: 2611)
     * - CNPJ (14 dígitos): CNPJ do emitente formatado com zeros à esquerda
     * - mod (2 dígitos): Modelo fiscal ("55" para NF-e ou "65" para NFC-e)
     * - serie (3 dígitos): Série do documento com zeros à esquerda
     * - nNF (9 dígitos): Número da nota fiscal com zeros à esquerda
     * - tpEmis (1 dígito): Tipo de emissão (1 = Normal, 9 = Contingência Offline)
     * - cNF (8 dígitos): Código numérico de segurança aleatório
     * - cDV (1 dígito): Dígito verificador calculado via Módulo 11
     */
    public String generateKey(
            String ufCode,
            LocalDate date,
            String cnpj,
            String model,
            int series,
            long invoiceNumber,
            int emissionType) {

        String cleanCnpj = cnpj != null ? cnpj.replaceAll("\\D", "") : "00000000000191";
        if (cleanCnpj.length() < 14) {
            cleanCnpj = String.format("%014d", Long.parseLong(cleanCnpj.isBlank() ? "1" : cleanCnpj));
        } else if (cleanCnpj.length() > 14) {
            cleanCnpj = cleanCnpj.substring(0, 14);
        }

        String uf = (ufCode != null && !ufCode.isBlank()) ? ufCode : "35"; // SP default
        if (uf.length() != 2) {
            uf = "35";
        }

        String aamm = (date != null ? date : LocalDate.now()).format(DateTimeFormatter.ofPattern("yyMM"));
        String mod = String.format("%02d", Integer.parseInt(model != null ? model : "55"));
        String ser = String.format("%03d", Math.max(1, series));
        String num = String.format("%09d", Math.max(1, invoiceNumber));
        String tpEmis = String.valueOf(emissionType > 0 ? emissionType : 1);
        String cnf = String.format("%08d", random.nextInt(90000000) + 10000000);

        String partialKey = uf + aamm + cleanCnpj + mod + ser + num + tpEmis + cnf;
        int checkDigit = calculateModulo11(partialKey);

        return partialKey + checkDigit;
    }

    /**
     * Calcula o dígito verificador (DV) pelo algoritmo Módulo 11 da SEFAZ.
     * Pesos de 2 a 9 da direita para a esquerda.
     */
    public static int calculateModulo11(String key43) {
        if (key43 == null || key43.length() != 43) {
            throw new IllegalArgumentException("A chave parcial para cálculo do DV deve conter exatamente 43 dígitos.");
        }

        int sum = 0;
        int weight = 2;

        for (int i = key43.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(key43.charAt(i));
            sum += digit * weight;
            weight++;
            if (weight > 9) {
                weight = 2;
            }
        }

        int remainder = sum % 11;
        if (remainder == 0 || remainder == 1) {
            return 0;
        } else {
            return 11 - remainder;
        }
    }

    /**
     * Valida a integridade de uma chave de acesso de 44 dígitos verificando o dígito Módulo 11.
     */
    public static boolean isValidKey(String accessKey) {
        if (accessKey == null || accessKey.length() != 44 || !accessKey.matches("\\d{44}")) {
            return false;
        }
        String partialKey = accessKey.substring(0, 43);
        int expectedDigit = calculateModulo11(partialKey);
        int actualDigit = Character.getNumericValue(accessKey.charAt(43));
        return expectedDigit == actualDigit;
    }
}

