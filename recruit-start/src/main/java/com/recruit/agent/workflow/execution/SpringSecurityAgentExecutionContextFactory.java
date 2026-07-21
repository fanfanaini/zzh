package com.recruit.agent.workflow.execution;

import com.recruit.agent.ai.infrastructure.AiProperties;
import com.recruit.agent.auth.application.AuthenticatedUser;
import com.recruit.agent.auth.domain.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SpringSecurityAgentExecutionContextFactory implements AgentExecutionContextFactory {
    private static final Map<UserRole, Set<String>> ROLE_PERMISSIONS = permissionsByRole();

    private final AiProperties aiProperties;

    public SpringSecurityAgentExecutionContextFactory(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @Override
    public AgentExecutionContext create(Long conversationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            Set<String> roles = user.roles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());
            Set<String> permissions = user.roles().stream()
                    .flatMap(role -> ROLE_PERMISSIONS.getOrDefault(role, Set.of()).stream())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            return context(conversationId, String.valueOf(user.userId()), roles, permissions,
                    Map.of("username", user.username()));
        }
        return context(conversationId, "local-system", Set.of("SYSTEM"),
                Set.of(AgentPermissions.RECRUIT_READ), Map.of("authentication", "local-or-system"));
    }

    private AgentExecutionContext context(Long conversationId, String principalId, Set<String> roles,
                                          Set<String> permissions, Map<String, String> attributes) {
        String model = aiProperties.isEnabled() ? aiProperties.getModel() : "disabled";
        return new AgentExecutionContext(UUID.randomUUID().toString(), conversationId, "default", null, principalId,
                roles, permissions, "runtime-resolved", model, attributes);
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
