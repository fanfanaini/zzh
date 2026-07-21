package com.recruit.agent.job.application;

import com.recruit.agent.screening.domain.EducationLevel;

import java.util.Set;

public record CreateJobCommand(
        String name,
        String description,
        Set<String> requiredSkills,
        Set<String> preferredSkills,
        EducationLevel educationRequirement,
        double minExperienceYears,
        double maxExperienceYears
) {
}
