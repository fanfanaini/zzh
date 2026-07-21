package com.recruit.agent.auth.application;

import com.recruit.agent.auth.domain.UserRole;

import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        String username,
        Set<UserRole> roles
) {
}
