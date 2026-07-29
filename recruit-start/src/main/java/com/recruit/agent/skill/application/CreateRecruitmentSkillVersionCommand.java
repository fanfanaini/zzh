package com.recruit.agent.skill.application;

import java.util.Set;

public record CreateRecruitmentSkillVersionCommand(
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
