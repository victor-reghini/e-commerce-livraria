package com.livraria.ecommerce.infrastructure.adapters.in.web.filter;

import com.livraria.ecommerce.domain.model.tenant.Tenant;
import com.livraria.ecommerce.domain.port.out.TenantRepositoryPort;
import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    public static final String TENANT_HEADER = "X-Tenant-ID";

    private final TenantRepositoryPort tenantRepository;

    public TenantFilter(TenantRepositoryPort tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 1. Tenta resolver via cabeçalho HTTP explícito (X-Tenant-ID)
            String tenantHeader = request.getHeader(TENANT_HEADER);
            if (StringUtils.hasText(tenantHeader)) {
                try {
                    UUID tenantId = UUID.fromString(tenantHeader.trim());
                    TenantContext.setTenantId(tenantId);
                    MDC.put(MdcLoggingFilter.MDC_TENANT_ID, tenantId.toString());
                    response.setHeader(TENANT_HEADER, tenantId.toString());
                } catch (IllegalArgumentException e) {
                    log.debug("Header X-Tenant-ID não é um UUID válido: {}", tenantHeader);
                }
            }

            // 2. Se não encontrou no header, tenta resolver pelo Host/domínio
            if (TenantContext.getTenantId().isEmpty()) {
                String host = request.getServerName();
                if (StringUtils.hasText(host) && !"localhost".equalsIgnoreCase(host) && !"127.0.0.1".equals(host)) {
                    tenantRepository.findByDomain(host).ifPresent(tenant -> {
                        UUID tenantId = tenant.getId();
                        TenantContext.setTenantId(tenantId);
                        MDC.put(MdcLoggingFilter.MDC_TENANT_ID, tenantId.toString());
                        response.setHeader(TENANT_HEADER, tenantId.toString());
                    });
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

