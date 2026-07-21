package com.recruit.agent.auth.api;

import com.recruit.agent.auth.application.AuthApplicationService;
import com.recruit.agent.auth.application.AuthenticatedUser;
import com.recruit.agent.auth.application.LoginCommand;
import com.recruit.agent.auth.application.RegisterUserCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationService service;

    public AuthController(AuthApplicationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccountResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserAccountResponse.from(service.register(new RegisterUserCommand(
                request.username(),
                request.password(),
                request.roles()
        )));
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthTokenResponse.from(service.login(new LoginCommand(request.username(), request.password())));
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return AuthTokenResponse.from(service.refresh(request.refreshToken()));
    }

    @GetMapping("/me")
    public CurrentUserResponse me(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("当前用户未登录");
        }
        return CurrentUserResponse.from(user);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            org.springframework.security.core.Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("当前用户未登录");
        }
        service.changePassword(user, request.currentPassword(), request.newPassword());
    }
}
