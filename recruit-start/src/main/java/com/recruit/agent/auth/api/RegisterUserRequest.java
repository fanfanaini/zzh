package com.recruit.agent.auth.api;

import com.recruit.agent.auth.domain.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterUserRequest(
        @NotBlank(message = "用户名不能为空")
        String username,
        @Size(min = 6, message = "密码长度至少为 6 位")
        String password,
        Set<UserRole> roles
) {
}
