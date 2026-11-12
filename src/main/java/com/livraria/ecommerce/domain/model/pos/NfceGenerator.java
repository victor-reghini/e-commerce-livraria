package com.livraria.ecommerce.domain.model.pos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public final class NfceGenerator {

    private static final Random RANDOM = new Random();

    private NfceGenerator() {}

    public static String generateAccessKey(String cnpjClean, int series, long number, boolean isContingency) {
        String uf = "35"; // SP default
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String cleanCnpj = (cnpjClean != null && cnpjClean.length() == 14)
                ? cnpjClean
                : "12345678000195";

        String model = "65"; // NFC-e
        String seriesStr = String.format("%03d", series % 1000);
        String numberStr = String.format("%09d", number % 1000000000L);
        String tpEmis = isContingency ? "9" : "1";
        String randomCode = String.format("%08d", RANDOM.nextInt(100000000));

        String raw43 = uf + dateStr + cleanCnpj + model + seriesStr + numberStr + tpEmis + randomCode;
        int checkDigit = calculateMod11(raw43);

        return raw43 + checkDigit;
    }

    private static int calculateMod11(String base) {
        int weight = 2;
        int sum = 0;
        for (int i = base.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(base.charAt(i));
            sum += digit * weight;
            weight = weight < 9 ? weight + 1 : 2;
        }
        int remainder = sum % 11;
        int dv = 11 - remainder;
        return (dv == 0 || dv >= 10) ? 0 : dv;
    }

    public static String buildSampleNfceXml(String accessKey, PosSale sale, String companyName, String cnpj) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<NFe xmlns=\"http://www.portalfiscal.inf.br/nfe\">");
        sb.append("<infNFe Id=\"NFe").append(accessKey).append("\" versao=\"4.00\">");
        sb.append("<ide><mod>65</mod><nNF>").append(sale.getSaleNumber()).append("</nNF><dhEmi>").append(sale.getCreatedAt()).append("</dhEmi></ide>");
        sb.append("<emit><CNPJ>").append(cnpj != null ? cnpj : "12345678000195").append("</CNPJ><xNome>").append(companyName != null ? companyName : "Livraria").append("</xNome></emit>");
        sb.append("<total><ICMSTot><vProd>").append(sale.getSubtotal()).append("</vProd><vDesc>").append(sale.getDiscount()).append("</vDesc><vNF>").append(sale.getTotal()).append("</vNF></ICMSTot></total>");
        sb.append("</infNFe>");
        sb.append("</NFe>");
        return sb.toString();
    }
}

