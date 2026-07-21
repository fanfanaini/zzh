package com.recruit.agent.auth.domain;

import java.time.Instant;
import java.util.Set;

public record UserAccount(
        Long id,
        String username,
        String passwordHash,
        Set<UserRole> roles,
        boolean enabled,
        Instant createdAt
) {

    public UserAccount {
        if (id == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        username = username.trim();
        if (roles == null || roles.isEmpty()) {
            roles = Set.of(UserRole.HR);
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UserAccount withPasswordHash(String newPasswordHash) {
        return new UserAccount(id, username, newPasswordHash, roles, enabled, createdAt);
    }
}
