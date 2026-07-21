package com.recruit.agent.auth.api;

import com.recruit.agent.auth.domain.UserAccount;
import com.recruit.agent.auth.domain.UserRole;

import java.time.Instant;
import java.util.Set;

public record UserAccountResponse(
        Long id,
        String username,
        Set<UserRole> roles,
        boolean enabled,
        Instant createdAt
) {

    public static UserAccountResponse from(UserAccount user) {
        return new UserAccountResponse(
                user.id(),
                user.username(),
                user.roles(),
                user.enabled(),
                user.createdAt()
        );
    }
}
