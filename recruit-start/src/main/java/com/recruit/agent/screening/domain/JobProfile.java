package com.recruit.agent.screening.domain;

import java.util.Set;

public record JobProfile(
        Long id,
        String jobName,
        Set<String> requiredSkills,
        Set<String> preferredSkills,
        EducationLevel educationRequirement,
        double minExperienceYears,
        Set<String> hardConstraints
) {

    public JobProfile {
        requiredSkills = normalize(requiredSkills);
        preferredSkills = normalize(preferredSkills);
        hardConstraints = normalize(hardConstraints);
        if (educationRequirement == null) {
            educationRequirement = EducationLevel.UNKNOWN;
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
