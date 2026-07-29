package com.recruit.agent.mcp.api;

import com.recruit.agent.auth.application.AuthenticatedUser;
import com.recruit.agent.auth.domain.UserRole;
import com.recruit.agent.tool.ToolInvocationContext;
import com.recruit.agent.workflow.execution.AgentPermissions;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class McpInvocationContextFactory {
    private static final Map<UserRole, Set<String>> ROLE_PERMISSIONS = permissionsByRole();

    private McpInvocationContextFactory() {
    }

    static ToolInvocationContext create(McpToolCallRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            Set<String> permissions = user.roles().stream()
                    .flatMap(role -> ROLE_PERMISSIONS.getOrDefault(role, Set.of()).stream())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            return new ToolInvocationContext(UUID.randomUUID().toString(), String.valueOf(user.userId()),
                    permissions, request == null ? null : request.idempotencyKey(),
                    Map.of("conversationId", request == null ? nullToZero(null) : nullToZero(request.conversationId()),
                            "username", user.username()));
        }
        return new ToolInvocationContext(UUID.randomUUID().toString(), "local-system",
                Set.of(AgentPermissions.RECRUIT_READ), request == null ? null : request.idempotencyKey(),
                Map.of("conversationId", request == null ? 0L : nullToZero(request.conversationId())));
    }

    private static Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static Map<UserRole, Set<String>> permissionsByRole() {
        Map<UserRole, Set<String>> permissions = new EnumMap<>(UserRole.class);
        permissions.put(UserRole.ADMIN, Set.of(AgentPermissions.RECRUIT_READ, AgentPermissions.RECRUIT_WRITE,
                AgentPermissions.RECRUIT_COMMUNICATE, AgentPermissions.AGENT_INSPECT));
        permissions.put(UserRole.RECRUITING_MANAGER, Set.of(AgentPermissions.RECRUIT_READ,
                AgentPermissions.RECRUIT_WRITE, AgentPermissions.RECRUIT_COMMUNICATE,
                AgentPermissions.AGENT_INSPECT));
        permissions.put(UserRole.HR, Set.of(AgentPermissions.RECRUIT_READ,
                AgentPermissions.RECRUIT_COMMUNICATE));
        permissions.put(UserRole.INTERVIEWER, Set.of(AgentPermissions.RECRUIT_READ));
        return Map.copyOf(permissions);
    }
}
