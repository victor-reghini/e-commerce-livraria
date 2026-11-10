package com.livraria.ecommerce.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Plataforma E-commerce, PDV e Gestão Multi-Tenant - API")
                        .version("1.0.0")
                        .description("API REST moderna para substituição de ecossistema legado WooCommerce/WordPress "
                                + "com isolamento multi-tenant, suporte a PDV offline/online, múltiplos estoques e alta concorrência.")
                        .contact(new Contact().name("Equipe de Engenharia").email("engenharia@livraria.com.br"))
                        .license(new License().name("Proprietary").url("https://livraria.com.br")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public OperationCustomizer customizeTenantHeader() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .required(false)
                    .name("X-Tenant-ID")
                    .description("UUID do Tenant para roteamento de contexto multi-tenant")
                    .example("00000000-0000-0000-0000-000000000001"));
            return operation;
        };
    }
}

