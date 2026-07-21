package com.recruit.agent.audit;

import com.recruit.agent.auth.application.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD", "OPTIONS");
    private final AuditLogService service;
    public AuditLoggingFilter(ObjectProvider<AuditLogService> service) { this.service = service.getIfAvailable(); }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            if (service != null && request.getRequestURI().startsWith("/api/") && !READ_METHODS.contains(request.getMethod())) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                AuthenticatedUser user = authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser value
                        ? value : null;
                try {
                    service.record(user == null ? null : user.userId(), user == null ? "anonymous" : user.username(),
                            request.getMethod(), request.getRequestURI(), response.getStatus(),
                            (System.nanoTime() - started) / 1_000_000, clientIp(request));
                } catch (RuntimeException ignored) {
                    // Audit persistence must never change the business response.
                }
            }
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }
}
