package com.recruit.agent.auth.application;

import com.recruit.agent.auth.domain.UserAccount;
import com.recruit.agent.auth.domain.UserAccountRepository;
import com.recruit.agent.auth.domain.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Set;

public class AuthApplicationService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthApplicationService(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserAccount register(RegisterUserCommand command) {
        if (command.password() == null || command.password().length() < 6) {
            throw new IllegalArgumentException("密码长度至少为 6 位");
        }
        repository.findByUsername(command.username()).ifPresent(user -> {
            throw new IllegalArgumentException("用户名已存在");
        });
        // Public registration always creates the least-privileged recruiting role.
        Set<UserRole> roles = Set.of(UserRole.HR);
        return repository.save(new UserAccount(
                repository.nextUserId(),
                command.username(),
                passwordEncoder.encode(command.password()),
                roles,
                true,
                Instant.now()
        ));
    }

    public AuthToken login(LoginCommand command) {
        UserAccount user = repository.findByUsername(command.username())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.enabled() || !passwordEncoder.matches(command.password(), user.passwordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return issueToken(user);
    }

    public AuthToken refresh(String refreshToken) {
        AuthenticatedUser tokenUser = jwtService.parseRefreshToken(refreshToken);
        UserAccount user = repository.findByUsername(tokenUser.username())
                .orElseThrow(() -> new IllegalArgumentException("刷新凭证无效"));
        if (!user.enabled() || !user.id().equals(tokenUser.userId())) {
            throw new IllegalArgumentException("刷新凭证无效");
        }
        return issueToken(user);
    }

    public void changePassword(AuthenticatedUser currentUser, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度至少为 6 位");
        }
        UserAccount user = repository.findByUsername(currentUser.username())
                .orElseThrow(() -> new IllegalArgumentException("当前用户不存在"));
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new IllegalArgumentException("当前密码错误");
        }
        if (passwordEncoder.matches(newPassword, user.passwordHash())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        repository.save(user.withPasswordHash(passwordEncoder.encode(newPassword)));
    }

    private AuthToken issueToken(UserAccount user) {
        return new AuthToken(
                "Bearer",
                jwtService.createToken(user),
                jwtService.createRefreshToken(user),
                jwtService.expiresInSeconds(),
                jwtService.refreshExpiresInSeconds(),
                user.id(),
                user.username(),
                user.roles()
        );
    }
}
