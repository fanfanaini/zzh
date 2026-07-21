package com.recruit.agent.candidate.application;

import com.recruit.agent.screening.domain.EducationLevel;

import java.util.Set;

public record RegisterCandidateCommand(
        String name,
        String phoneHash,
        String emailHash,
        EducationLevel educationLevel,
        double totalExperienceYears,
        String expectedCity,
        Set<String> skills
) {
}
