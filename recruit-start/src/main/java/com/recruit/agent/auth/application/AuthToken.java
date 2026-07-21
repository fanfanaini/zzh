package com.recruit.agent.auth.application;

import com.recruit.agent.auth.domain.UserRole;

import java.util.Set;

public record AuthToken(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        long refreshExpiresInSeconds,
        Long userId,
        String username,
        Set<UserRole> roles
) {
}
