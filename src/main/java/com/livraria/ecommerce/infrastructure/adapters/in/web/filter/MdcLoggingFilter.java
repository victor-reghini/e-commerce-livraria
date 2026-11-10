package com.livraria.ecommerce.infrastructure.adapters.in.web.filter;

import com.livraria.ecommerce.infrastructure.adapters.out.persistence.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID = "correlation_id";
    public static final String MDC_TENANT_ID = "tenant_id";
    public static final String MDC_USER_ID = "user_id";
    public static final String MDC_ENVIRONMENT = "environment";

    @Value("${app.environment:development}")
    private String environment;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_ENVIRONMENT, environment);

        // Preenche tenant_id caso já tenha sido resolvido
        TenantContext.getTenantId().ifPresent(id -> MDC.put(MDC_TENANT_ID, id.toString()));

        // Preenche user_id caso usuário já esteja autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            MDC.put(MDC_USER_ID, auth.getName());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ENVIRONMENT);
        }
    }
}

