package com.recruit.agent.auth.application;

import com.recruit.agent.auth.domain.UserRole;
import com.recruit.agent.auth.infrastructure.InMemoryUserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthApplicationServiceTest {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(
            "test-secret-test-secret-test-secret-123456",
            3600,
            Clock.fixed(Instant.parse("2026-07-11T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void registersAndLogsInUser() {
        AuthApplicationService service = new AuthApplicationService(
                new InMemoryUserAccountRepository(passwordEncoder),
                passwordEncoder,
                jwtService
        );
        service.register(new RegisterUserCommand("hr1", "secret123", Set.of(UserRole.HR)));

        AuthToken token = service.login(new LoginCommand("hr1", "secret123"));

        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.accessToken()).isNotBlank();
        assertThat(token.roles()).containsExactly(UserRole.HR);
    }

    @Test
    void rejectsWrongPassword() {
        AuthApplicationService service = new AuthApplicationService(
                new InMemoryUserAccountRepository(passwordEncoder),
                passwordEncoder,
                jwtService
        );

        assertThatThrownBy(() -> service.login(new LoginCommand("admin", "bad-password")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    void refreshesAccessTokenAndChangesPassword() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository(passwordEncoder);
        AuthApplicationService service = new AuthApplicationService(repository, passwordEncoder, jwtService);
        var registered = service.register(new RegisterUserCommand("hr2", "secret123", Set.of(UserRole.HR)));

        AuthToken login = service.login(new LoginCommand("hr2", "secret123"));
        AuthToken refreshed = service.refresh(login.refreshToken());
        service.changePassword(
                new AuthenticatedUser(registered.id(), registered.username(), registered.roles()),
                "secret123",
                "new-secret"
        );

        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(service.login(new LoginCommand("hr2", "new-secret")).accessToken()).isNotBlank();
        assertThatThrownBy(() -> service.login(new LoginCommand("hr2", "secret123")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
