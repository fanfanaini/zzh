package com.recruit.agent.skill.domain;

import java.time.Instant;
import java.util.Set;

public record RecruitmentSkill(
        Long id,
        String name,
        String category,
        String description,
        String scenario,
        Set<String> triggerKeywords,
        int priority,
        RecruitmentSkillStatus status,
        Long currentVersionId,
        Instant createdAt,
        Instant updatedAt
) {
    public RecruitmentSkill {
        if (id == null) throw new IllegalArgumentException("skill id must not be null");
        name = required(name, "skill name");
        category = required(category, "skill category");
        description = required(description, "skill description");
        scenario = required(scenario, "skill scenario");
        triggerKeywords = triggerKeywords == null ? Set.of() : Set.copyOf(triggerKeywords);
        status = status == null ? RecruitmentSkillStatus.DRAFT : status;
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public boolean enabled() {
        return status == RecruitmentSkillStatus.ENABLED && currentVersionId != null;
    }

    public RecruitmentSkill publish(Long versionId, Instant at) {
        return new RecruitmentSkill(id, name, category, description, scenario, triggerKeywords, priority,
                RecruitmentSkillStatus.ENABLED, versionId, createdAt, at == null ? Instant.now() : at);
    }

    public RecruitmentSkill disable(Instant at) {
        return new RecruitmentSkill(id, name, category, description, scenario, triggerKeywords, priority,
                RecruitmentSkillStatus.DISABLED, currentVersionId, createdAt, at == null ? Instant.now() : at);
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }
}
