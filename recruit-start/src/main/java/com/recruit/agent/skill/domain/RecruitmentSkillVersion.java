package com.recruit.agent.skill.domain;

import java.time.Instant;
import java.util.Set;

public record RecruitmentSkillVersion(
        Long id,
        Long skillId,
        String version,
        String instruction,
        String promptFragment,
        String fewShots,
        String guardrails,
        Set<String> allowedTools,
        Set<String> riskTags,
        String handoffPolicy,
        boolean published,
        Instant createdAt,
        Instant publishedAt
) {
    public RecruitmentSkillVersion {
        if (id == null) throw new IllegalArgumentException("skill version id must not be null");
        if (skillId == null) throw new IllegalArgumentException("skill id must not be null");
        version = required(version, "skill version");
        instruction = required(instruction, "skill instruction");
        promptFragment = normalize(promptFragment);
        fewShots = normalize(fewShots);
        guardrails = normalize(guardrails);
        allowedTools = allowedTools == null ? Set.of() : Set.copyOf(allowedTools);
        riskTags = riskTags == null ? Set.of() : Set.copyOf(riskTags);
        handoffPolicy = normalize(handoffPolicy);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public RecruitmentSkillVersion publish(Instant at) {
        Instant publishedTime = at == null ? Instant.now() : at;
        return new RecruitmentSkillVersion(id, skillId, version, instruction, promptFragment, fewShots, guardrails,
                allowedTools, riskTags, handoffPolicy, true, createdAt, publishedTime);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
