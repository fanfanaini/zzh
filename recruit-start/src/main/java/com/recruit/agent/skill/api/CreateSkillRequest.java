package com.recruit.agent.skill.api;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateSkillRequest(
        @NotBlank String name,
        @NotBlank String category,
        @NotBlank String description,
        @NotBlank String scenario,
        Set<String> triggerKeywords,
        int priority,
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
