package com.recruit.agent.auth.infrastructure;

import com.recruit.agent.auth.application.JwtService;
import com.recruit.agent.auth.domain.UserAccount;
import com.recruit.agent.auth.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {
    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsUnauthorizedAndStopsChainWhenTokenExpired() throws Exception {
        Instant issuedAt = Instant.parse("2026-07-11T00:00:00Z");
        JwtService issuer = new JwtService(SECRET, 60, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = issuer.createToken(new UserAccount(1L, "hr", "hash", Set.of(UserRole.HR), true, issuedAt));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                new JwtService(SECRET, 60, Clock.fixed(issuedAt.plusSeconds(61), ZoneOffset.UTC)));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("重新登录");
        assertThat(chainInvoked).isFalse();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void skipsExpiredAccessTokenForLoginEndpoint() throws Exception {
        Instant issuedAt = Instant.parse("2026-07-11T00:00:00Z");
        JwtService issuer = new JwtService(SECRET, 60, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = issuer.createToken(new UserAccount(1L, "hr", "hash", Set.of(UserRole.HR), true, issuedAt));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                new JwtService(SECRET, 60, Clock.fixed(issuedAt.plusSeconds(61), ZoneOffset.UTC)));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> chainInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chainInvoked).isTrue();
    }
}
