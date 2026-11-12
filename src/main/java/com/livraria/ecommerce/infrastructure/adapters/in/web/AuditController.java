package com.livraria.ecommerce.infrastructure.adapters.in.web;

import com.livraria.ecommerce.application.dto.audit.AuditDto;
import com.livraria.ecommerce.domain.model.audit.AuditLog;
import com.livraria.ecommerce.domain.port.in.AuditUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Auditoria e Compliance LGPD", description = "Trilha de auditoria imutável com mascaramento de CPF/cartão e isolamento RLS")
public class AuditController {

    private final AuditUseCase auditUseCase;

    public AuditController(AuditUseCase auditUseCase) {
        this.auditUseCase = auditUseCase;
    }

    @GetMapping("/logs")
    @PreAuthorize("hasAnyAuthority('AUDIT_VIEW', 'ADMIN', 'ROLE_TENANT_ADMIN', 'ROLE_SUPER_ADMIN')")
    @Operation(summary = "Consultar Trilha de Auditoria com Filtros e Mascaramento LGPD/PCI-DSS",
            description = "Retorna logs de auditoria imutáveis com mascaramento automático de CPF, senhas e tokens de cartão.")
    public ResponseEntity<AuditDto.AuditLogPageResponse> queryLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<AuditLog> logs = auditUseCase.queryLogs(action, resourceType, from, to, page, size);
        long total = auditUseCase.countLogs(action, resourceType, from, to);

        List<AuditDto.AuditLogResponse> responseLogs = logs.stream()
                .map(AuditDto.AuditLogResponse::fromDomain)
                .toList();

        return ResponseEntity.ok(new AuditDto.AuditLogPageResponse(responseLogs, total, page, size));
    }
}
