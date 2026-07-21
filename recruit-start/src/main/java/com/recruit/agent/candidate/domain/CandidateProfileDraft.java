package com.recruit.agent.candidate.domain;

import com.recruit.agent.screening.domain.EducationLevel;

import java.util.Set;

public record CandidateProfileDraft(
        EducationLevel educationLevel,
        double totalExperienceYears,
        String expectedCity,
        Set<String> skills
) {

    public CandidateProfileDraft {
        if (educationLevel == null) {
            educationLevel = EducationLevel.UNKNOWN;
        }
        if (totalExperienceYears < 0) {
            throw new IllegalArgumentException("工作年限不能为负数");
        }
        skills = normalize(skills);
    }

    private static Set<String> normalize(Set<String> values) {
        if (values == null) {
            return Set.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
