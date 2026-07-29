package com.recruit.agent.skill.api;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateSkillVersionRequest(
        @NotBlank String version,
        @NotBlank String instruction,
        String promptFragment,
        String fewShots,
        String guardrails,
        Set<String> allowedTools,
        Set<String> riskTags,
        String handoffPolicy,
        Boolean publish
) {
}
