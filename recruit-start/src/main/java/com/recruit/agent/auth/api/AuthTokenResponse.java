package com.recruit.agent.auth.api;

import com.recruit.agent.auth.application.AuthToken;
import com.recruit.agent.auth.domain.UserRole;

import java.util.Set;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        long refreshExpiresInSeconds,
        Long userId,
        String username,
        Set<UserRole> roles
) {

    public static AuthTokenResponse from(AuthToken token) {
        return new AuthTokenResponse(
                token.tokenType(),
                token.accessToken(),
                token.refreshToken(),
                token.expiresInSeconds(),
                token.refreshExpiresInSeconds(),
                token.userId(),
                token.username(),
                token.roles()
        );
    }
}
