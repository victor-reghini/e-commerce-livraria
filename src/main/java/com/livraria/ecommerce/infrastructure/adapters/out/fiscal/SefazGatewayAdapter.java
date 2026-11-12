package com.livraria.ecommerce.infrastructure.adapters.out.fiscal;

import com.livraria.ecommerce.domain.model.fiscal.FiscalCancellationResult;
import com.livraria.ecommerce.domain.model.fiscal.SefazAuthResult;
import com.livraria.ecommerce.domain.model.fiscal.SefazStatusResult;
import com.livraria.ecommerce.domain.port.out.SefazGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Random;

@Component
public class SefazGatewayAdapter implements SefazGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SefazGatewayAdapter.class);

    private final NfeXmlBuilder nfeXmlBuilder;
    private final Random random = new Random();

    @Value("${app.sefaz.environment:homologation}")
    private String sefazEnvironment;

    @Value("${app.sefaz.force-contingency:false}")
    private boolean forceContingency;

    public SefazGatewayAdapter(NfeXmlBuilder nfeXmlBuilder) {
        this.nfeXmlBuilder = nfeXmlBuilder;
    }

    @Override
    public SefazStatusResult checkServiceStatus(String uf, String environment) {
        String targetUf = (uf != null && !uf.isBlank()) ? uf.toUpperCase() : "SP";
        String targetEnv = (environment != null && !environment.isBlank()) ? environment : sefazEnvironment;

        long startTime = System.currentTimeMillis();
        try {
            // Simulação de latência de rede com o WebService da SEFAZ
            long latency = 45L + random.nextInt(40);
            Thread.sleep(latency);

            long totalTime = System.currentTimeMillis() - startTime;
            if (forceContingency) {
                return SefazStatusResult.degraded(targetUf, targetEnv, "Ambiente em manutenção preventiva pela SEFAZ", totalTime);
            }
            return SefazStatusResult.operational(targetUf, targetEnv, totalTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SefazStatusResult.degraded(targetUf, targetEnv, "Timeout na consulta do WebService SEFAZ", 5000L);
        }
    }

    @Override
    public SefazAuthResult authorizeInvoice(String xmlPayload, String accessKey, String model, String uf) {
        log.info("Iniciando transmissão de lote fiscal para SEFAZ. Modelo: {}, Chave: {}, UF: {}", model, accessKey, uf);

        if (forceContingency) {
            log.warn("SEFAZ indisponível ou em manutenção. Ativando contingência offline para a chave: {}", accessKey);
            return SefazAuthResult.contingency(accessKey, "Indisponibilidade temporária de WebService SEFAZ autorizadora", xmlPayload);
        }

        try {
            // 1. Gera Digest Value via SHA-1 dos dados XML
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digestBytes = sha1.digest(xmlPayload.getBytes(StandardCharsets.UTF_8));
            String digestValue = Base64.getEncoder().encodeToString(digestBytes);

            // 2. Simula número de protocolo oficial SEFAZ com 15 dígitos (ex: 135260001234567)
            // Estrutura: [1] (tpAmb) + [35] (cUF) + [26] (ano) + [10 dígitos aleatórios]
            String ufPrefix = accessKey != null && accessKey.length() >= 2 ? accessKey.substring(0, 2) : "35";
            long seq10 = 1000000000L + (long) (random.nextDouble() * 8999999999L);
            String protocolNumber = "1" + ufPrefix + "26" + seq10;

            // 3. Envelope nfeProc
            String processedXml = nfeXmlBuilder.wrapWithProtocol(xmlPayload, protocolNumber, digestValue, accessKey);

            log.info("Lote fiscal AUTORIZADO com sucesso pela SEFAZ. Protocolo: {}", protocolNumber);
            return SefazAuthResult.success(protocolNumber, accessKey, digestValue, processedXml);

        } catch (Exception e) {
            log.error("Erro na comunicação com o WebService SEFAZ: {}", e.getMessage(), e);
            return SefazAuthResult.contingency(accessKey, "Falha de conexão com SEFAZ: " + e.getMessage(), xmlPayload);
        }
    }

    @Override
    public FiscalCancellationResult cancelInvoice(String accessKey, String protocolNumber, String reason, String uf) {
        log.info("Transmitindo evento de Cancelamento para a SEFAZ. Chave: {}, Protocolo: {}, Motivo: {}", accessKey, protocolNumber, reason);

        if (reason == null || reason.trim().length() < 15) {
            return FiscalCancellationResult.failure(494, "Rejeição: Justificativa de cancelamento com menos de 15 caracteres", accessKey);
        }

        String ufPrefix = accessKey != null && accessKey.length() >= 2 ? accessKey.substring(0, 2) : "35";
        long seq10 = 1000000000L + (long) (random.nextDouble() * 8999999999L);
        String eventProtocol = "1" + ufPrefix + "26" + seq10;

        return FiscalCancellationResult.success(eventProtocol, accessKey);
    }
}
