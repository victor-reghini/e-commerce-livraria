package com.livraria.ecommerce.infrastructure.adapters.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@Tag(name = "Documentação da Plataforma", description = "Portal interativo de documentação técnica, arquitetura, diagramas UML e especificação completa de endpoints")
public class DocumentationController {

    private String cachedHtmlDocs = null;

    @GetMapping(value = {"/docs", "/documentation"}, produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Página HTML interativa de documentação técnica completa da API com diagramas UML")
    public ResponseEntity<String> getHtmlDocumentation() throws IOException {
        if (cachedHtmlDocs == null) {
            Resource resource = new ClassPathResource("static/docs/index.html");
            if (resource.exists()) {
                cachedHtmlDocs = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            } else {
                return ResponseEntity.status(404).body("<h1>Documentação não encontrada no classpath</h1>");
            }
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/html; charset=UTF-8"))
                .body(cachedHtmlDocs);
    }
}

