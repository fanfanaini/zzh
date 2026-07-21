package com.recruit.agent.candidate.domain;

import com.recruit.agent.screening.domain.EducationLevel;

import java.time.Instant;
import java.util.Set;

public record Candidate(
        Long id,
        String name,
        String phoneHash,
        String emailHash,
        EducationLevel educationLevel,
        double totalExperienceYears,
        String expectedCity,
        Set<String> skills,
        Instant createdAt,
        Instant updatedAt,
        CandidateStatus status
) implements java.io.Serializable {

    public Candidate(Long id, String name, String phoneHash, String emailHash, EducationLevel educationLevel,
                     double totalExperienceYears, String expectedCity, Set<String> skills, Instant createdAt) {
        this(id, name, phoneHash, emailHash, educationLevel, totalExperienceYears, expectedCity, skills,
                createdAt, createdAt, CandidateStatus.ACTIVE);
    }

    public Candidate {
        if (id == null) {
            throw new IllegalArgumentException("候选人 ID 不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("候选人姓名不能为空");
        }
        if (totalExperienceYears < 0) {
            throw new IllegalArgumentException("工作年限不能为负数");
        }
        if (educationLevel == null) {
            educationLevel = EducationLevel.UNKNOWN;
        }
        skills = normalize(skills);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (status == null) {
            status = CandidateStatus.ACTIVE;
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

    public Candidate withProfile(CandidateProfileDraft profile) {
        return new Candidate(
                id,
                name,
                phoneHash,
                emailHash,
                profile.educationLevel(),
                profile.totalExperienceYears(),
                profile.expectedCity(),
                profile.skills(),
                createdAt,
                Instant.now(),
                status
        );
    }

    public Candidate update(String newName, String newPhoneHash, String newEmailHash, EducationLevel newEducationLevel,
                            double newTotalExperienceYears, String newExpectedCity, Set<String> newSkills) {
        return new Candidate(id, newName, newPhoneHash, newEmailHash, newEducationLevel,
                newTotalExperienceYears, newExpectedCity, newSkills, createdAt, Instant.now(), status);
    }

    public Candidate disable() {
        return withStatus(CandidateStatus.DISABLED);
    }

    public Candidate markDeleted() {
        return withStatus(CandidateStatus.DELETED);
    }

    private Candidate withStatus(CandidateStatus newStatus) {
        return new Candidate(id, name, phoneHash, emailHash, educationLevel, totalExperienceYears,
                expectedCity, skills, createdAt, Instant.now(), newStatus);
    }
}
