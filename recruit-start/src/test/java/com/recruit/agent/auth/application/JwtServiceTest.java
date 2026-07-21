package com.recruit.agent.auth.application;

import com.recruit.agent.auth.domain.UserAccount;
import com.recruit.agent.auth.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-test-secret-test-secret-123456",
            3600,
            Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void createsAndParsesToken() {
        UserAccount user = new UserAccount(
                1L,
                "hr",
                "hash",
                Set.of(UserRole.HR),
                true,
                Instant.parse("2026-07-11T00:00:00Z")
        );

        AuthenticatedUser parsed = jwtService.parse(jwtService.createToken(user));

        assertThat(parsed.userId()).isEqualTo(1L);
        assertThat(parsed.username()).isEqualTo("hr");
        assertThat(parsed.roles()).containsExactly(UserRole.HR);
    }

    @Test
    void rejectsInvalidToken() {
        assertThatThrownBy(() -> jwtService.parse("bad.token.value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void separatesAccessAndRefreshTokens() {
        UserAccount user = new UserAccount(1L, "hr", "hash", Set.of(UserRole.HR), true, Instant.now());
        String refreshToken = jwtService.createRefreshToken(user);

        assertThat(jwtService.parseRefreshToken(refreshToken).username()).isEqualTo("hr");
        assertThatThrownBy(() -> jwtService.parse(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("类型");
    }
}
