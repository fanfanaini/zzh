package com.recruit.agent.screening.domain;

import java.util.Set;

public record CandidateProfile(
        Long id,
        String name,
        Set<String> skills,
        EducationLevel educationLevel,
        double totalExperienceYears,
        Set<String> projectKeywords
) {

    public CandidateProfile {
        skills = normalize(skills);
        projectKeywords = normalize(projectKeywords);
        if (educationLevel == null) {
            educationLevel = EducationLevel.UNKNOWN;
        }
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
