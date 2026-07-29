package com.recruit.agent.skill.application;

import java.util.Set;

public record CreateRecruitmentSkillCommand(
        String name,
        String category,
        String description,
        String scenario,
        Set<String> triggerKeywords,
        int priority,
        String version,
        String instruction,
        String promptFragment,
        String fewShots,
        String guardrails,
        Set<String> allowedTools,
        Set<String> riskTags,
        String handoffPolicy,
        boolean publish
) {
}
