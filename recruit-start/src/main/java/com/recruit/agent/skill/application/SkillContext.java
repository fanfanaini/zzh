package com.recruit.agent.skill.application;

import java.util.List;

public record SkillContext(
        String promptContext,
        List<SkillMatch> matches,
        List<String> allowedTools,
        List<String> riskTags
) {
    public static SkillContext empty() {
        return new SkillContext("", List.of(), List.of(), List.of());
    }

    public boolean emptyContext() {
        return promptContext == null || promptContext.isBlank();
    }
}
