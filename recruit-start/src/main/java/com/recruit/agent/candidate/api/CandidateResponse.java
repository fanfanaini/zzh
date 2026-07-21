package com.recruit.agent.candidate.api;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateStatus;
import com.recruit.agent.screening.domain.EducationLevel;

import java.time.Instant;
import java.util.Set;

public record CandidateResponse(
        Long id,
        String name,
        String phoneHash,
        String emailHash,
        EducationLevel educationLevel,
        double totalExperienceYears,
        String expectedCity,
        Set<String> skills,
        CandidateStatus status,
        Instant updatedAt,
        Instant createdAt
) {

    public static CandidateResponse from(Candidate candidate) {
        return new CandidateResponse(
                candidate.id(),
                candidate.name(),
                candidate.phoneHash(),
                candidate.emailHash(),
                candidate.educationLevel(),
                candidate.totalExperienceYears(),
                candidate.expectedCity(),
                candidate.skills(),
                candidate.status(),
                candidate.updatedAt(),
                candidate.createdAt()
        );
    }
}
