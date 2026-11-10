package com.livraria.ecommerce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LivrariaApplication {

    private static final Logger log = LoggerFactory.getLogger(LivrariaApplication.class);

    public static void main(String[] args) {
        log.info("Iniciando Plataforma E-commerce & PDV Multi-Tenant (Java 21 LTS / Virtual Threads)...");
        SpringApplication.run(LivrariaApplication.class, args);
        log.info("Plataforma inicializada com sucesso e pronta para receber requisições.");
    }
}

