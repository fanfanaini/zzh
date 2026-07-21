package com.recruit.agent.workflow.execution;

import com.recruit.agent.ai.infrastructure.AiProperties;
import com.recruit.agent.auth.application.AuthenticatedUser;
import com.recruit.agent.auth.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SpringSecurityAgentExecutionContextFactoryTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void derivesIdentityRolesAndPermissionsFromAuthenticatedUser() {
        AuthenticatedUser user = new AuthenticatedUser(42L, "hr-user", Set.of(UserRole.HR));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_HR"))));
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setModel("deepseek-chat");

        var context = new SpringSecurityAgentExecutionContextFactory(properties).create(9L);

        assertThat(context.conversationId()).isEqualTo(9L);
        assertThat(context.principalId()).isEqualTo("42");
        assertThat(context.roles()).containsExactly("HR");
        assertThat(context.permissions()).contains(AgentPermissions.RECRUIT_READ,
                AgentPermissions.RECRUIT_COMMUNICATE);
        assertThat(context.modelVersion()).isEqualTo("deepseek-chat");
        assertThat(context.attributes()).containsEntry("username", "hr-user");
    }

    @Test
    void suppliesRestrictedSystemContextWithoutAuthentication() {
        var context = new SpringSecurityAgentExecutionContextFactory(new AiProperties()).create(10L);

        assertThat(context.principalId()).isEqualTo("local-system");
        assertThat(context.roles()).containsExactly("SYSTEM");
        assertThat(context.permissions()).containsExactly(AgentPermissions.RECRUIT_READ);
        assertThat(context.modelVersion()).isEqualTo("disabled");
    }
}
