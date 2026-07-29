package com.recruit.agent.skill.application;

import com.recruit.agent.skill.domain.RecruitmentSkillCard;

public record SkillMatch(
        Long skillId,
        String name,
        String category,
        String version,
        double score,
        String reason,
        RecruitmentSkillCard card
) {
}
