package com.recruit.agent.candidate.api;

import com.recruit.agent.screening.domain.EducationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Set;

public record RegisterCandidateRequest(
        @NotBlank(message = "候选人姓名不能为空")
        String name,
        String phoneHash,
        String emailHash,
        EducationLevel educationLevel,
        @PositiveOrZero(message = "工作年限不能为负数")
        double totalExperienceYears,
        String expectedCity,
        Set<String> skills
) {
}
