package com.recruit.agent.skill.domain;

import java.util.Set;

public record RecruitmentSkillCard(
        Long id,
        String name,
        String category,
        String description,
        String scenario,
        Set<String> triggerKeywords,
        int priority,
        String version
) {
}
