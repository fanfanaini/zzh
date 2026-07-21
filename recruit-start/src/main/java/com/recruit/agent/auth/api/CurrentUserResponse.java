package com.recruit.agent.auth.api;

import com.recruit.agent.auth.application.AuthenticatedUser;
import com.recruit.agent.auth.domain.UserRole;

import java.util.Set;

public record CurrentUserResponse(
        Long userId,
        String username,
        Set<UserRole> roles,
        Set<String> permissions
) {
    public static CurrentUserResponse from(AuthenticatedUser user) {
        return new CurrentUserResponse(user.userId(), user.username(), user.roles(), permissions(user.roles()));
    }

    private static Set<String> permissions(Set<UserRole> roles) {
        if (roles.contains(UserRole.ADMIN)) {
            return Set.of("JOB_MANAGE", "CANDIDATE_MANAGE", "RESUME_PARSE", "SCREENING_RUN",
                    "CONVERSATION_MANAGE", "KNOWLEDGE_MANAGE", "PROMPT_MANAGE", "AUDIT_READ", "MODEL_CALL_READ");
        }
        if (roles.contains(UserRole.RECRUITING_MANAGER)) {
            return Set.of("JOB_MANAGE", "CANDIDATE_MANAGE", "RESUME_PARSE", "SCREENING_RUN",
                    "CONVERSATION_MANAGE", "KNOWLEDGE_MANAGE");
        }
        if (roles.contains(UserRole.HR)) {
            return Set.of("JOB_READ", "CANDIDATE_MANAGE", "RESUME_PARSE", "SCREENING_RUN", "CONVERSATION_MANAGE");
        }
        return Set.of("JOB_READ", "CANDIDATE_READ", "SCREENING_READ");
    }
}
