package com.recruit.agent.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.auth.infrastructure.AuthProperties;
import com.recruit.agent.auth.infrastructure.JwtAuthenticationFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

import java.util.Map;
import com.recruit.agent.audit.AuditLoggingFilter;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthProperties properties,
            ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
            AuditLoggingFilter auditLoggingFilter
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(auditLoggingFilter, UsernamePasswordAuthenticationFilter.class);
        if (!properties.isEnabled()) {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll()).build();
        }
        JwtAuthenticationFilter jwtAuthenticationFilter = jwtAuthenticationFilterProvider.getIfAvailable();
        if (jwtAuthenticationFilter == null) {
            throw new IllegalStateException("JWT 鉴权已开启，但 JwtAuthenticationFilter 未配置");
        }
        return http
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh",
                                "/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit/**", "/api/prompts/**", "/api/model-calls/**",
                                "/api/agent-traces/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/knowledge/**").hasAnyRole("ADMIN", "RECRUITING_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/knowledge/**").hasAnyRole("ADMIN", "HR", "RECRUITING_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/replies/*/approve", "/api/replies/*/reject")
                        .hasAnyRole("ADMIN", "HR", "RECRUITING_MANAGER")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        ObjectMapper objectMapper = new ObjectMapper();
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of("message", "未认证或登录已过期"));
        };
    }
}
