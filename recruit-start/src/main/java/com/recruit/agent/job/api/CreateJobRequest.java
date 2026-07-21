package com.recruit.agent.job.api;

import com.recruit.agent.screening.domain.EducationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Set;

public record CreateJobRequest(
        @NotBlank(message = "岗位名称不能为空")
        String name,
        @NotBlank(message = "岗位描述不能为空")
        String description,
        Set<String> requiredSkills,
        Set<String> preferredSkills,
        EducationLevel educationRequirement,
        @PositiveOrZero(message = "最低工作年限不能为负数")
        double minExperienceYears,
        @PositiveOrZero(message = "最高工作年限不能为负数")
        double maxExperienceYears
) {
}
