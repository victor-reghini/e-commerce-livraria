package com.livraria.ecommerce.infrastructure.adapters.out.shipping;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingCarrier;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingQuote;
import com.livraria.ecommerce.domain.model.ecommerce.ShippingService;
import com.livraria.ecommerce.domain.port.out.ShippingCalculatorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ShippingCalculatorAdapter implements ShippingCalculatorPort {

    private static final Logger log = LoggerFactory.getLogger(ShippingCalculatorAdapter.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ShippingCalculatorAdapter(@Autowired(required = false) StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ShippingQuote> calculate(UUID tenantId, String originZip, String destinationZip,
                                         int totalWeightGrams, double height, double width, double depth,
                                         BigDecimal declaredValue) {
        String cleanOrigin = originZip != null ? originZip.replaceAll("\\D", "") : "01001000";
        String cleanDest = destinationZip != null ? destinationZip.replaceAll("\\D", "") : "";

        // Cálculo de peso cúbico padrão Correios/Melhor Envio (fator 6000)
        double cubicKg = (height * width * depth) / 6000.0;
        int cubicGrams = (int) (cubicKg * 1000);
        int finalWeightGrams = Math.max(totalWeightGrams, cubicGrams);
        if (finalWeightGrams <= 0) {
            finalWeightGrams = 300; // peso padrão de livro médio
        }

        String cacheKey = String.format("shipping:quote:%s:%s:%s:%d:%s",
                tenantId, cleanOrigin, cleanDest, finalWeightGrams, declaredValue);

        // 1. Tentar recuperar do cache Redis
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    log.debug("Cotação de frete recuperada do cache Redis: {}", cacheKey);
                    return objectMapper.readValue(cachedJson, new TypeReference<List<ShippingQuote>>() {});
                }
            } catch (Exception e) {
                log.warn("Falha ao consultar cache Redis de frete: {}", e.getMessage());
            }
        }

        // 2. Calcular cotações multi-transportadora
        List<ShippingQuote> quotes = new ArrayList<>();
        double weightFactor = finalWeightGrams / 500.0;

        // Correios SEDEX
        BigDecimal sedexPrice = BigDecimal.valueOf(22.50 + (weightFactor * 3.50)).setScale(2, RoundingMode.HALF_UP);
        quotes.add(ShippingQuote.available(ShippingCarrier.CORREIOS, ShippingService.SEDEX, "Correios SEDEX", sedexPrice, 2));

        // Correios PAC
        BigDecimal pacPrice = BigDecimal.valueOf(14.90 + (weightFactor * 2.00)).setScale(2, RoundingMode.HALF_UP);
        quotes.add(ShippingQuote.available(ShippingCarrier.CORREIOS, ShippingService.PAC, "Correios PAC", pacPrice, 6));

        // Melhor Envio Expresso (Jadlog .Package / Azul Cargo)
        BigDecimal meExpPrice = BigDecimal.valueOf(19.80 + (weightFactor * 3.00)).setScale(2, RoundingMode.HALF_UP);
        quotes.add(ShippingQuote.available(ShippingCarrier.MELHOR_ENVIO, ShippingService.MELHOR_ENVIO_EXPRESSO, "Melhor Envio Expresso", meExpPrice, 3));

        // Melhor Envio Econômico (Loggi / Buslog)
        BigDecimal meEcoPrice = BigDecimal.valueOf(12.50 + (weightFactor * 1.80)).setScale(2, RoundingMode.HALF_UP);
        quotes.add(ShippingQuote.available(ShippingCarrier.MELHOR_ENVIO, ShippingService.MELHOR_ENVIO_ECONOMICO, "Melhor Envio Econômico", meEcoPrice, 7));

        // Retirada na Loja / Balcão
        quotes.add(ShippingQuote.available(ShippingCarrier.RETIRADA, ShippingService.RETIRADA_BALCAO, "Retirada no Balcão da Loja", BigDecimal.ZERO, 0));

        // 3. Salvar no cache Redis com TTL de 60 minutos
        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(quotes);
                redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(60));
            } catch (Exception e) {
                log.warn("Falha ao armazenar cotação no cache Redis: {}", e.getMessage());
            }
        }

        return quotes;
    }
}

