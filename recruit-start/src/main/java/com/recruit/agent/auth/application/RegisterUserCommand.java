package com.recruit.agent.auth.application;

import com.recruit.agent.auth.domain.UserRole;

import java.util.Set;

public record RegisterUserCommand(
        String username,
        String password,
        Set<UserRole> roles
) {
}
