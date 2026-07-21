package com.recruit.agent.job.domain;

import com.recruit.agent.screening.domain.EducationLevel;

import java.time.Instant;
import java.util.Set;

public record Job(
        Long id,
        String name,
        String description,
        Set<String> requiredSkills,
        Set<String> preferredSkills,
        EducationLevel educationRequirement,
        double minExperienceYears,
        double maxExperienceYears,
        Instant createdAt,
        Instant updatedAt,
        JobStatus status
) {

    public Job(Long id, String name, String description, Set<String> requiredSkills, Set<String> preferredSkills,
               EducationLevel educationRequirement, double minExperienceYears, double maxExperienceYears,
               Instant createdAt) {
        this(id, name, description, requiredSkills, preferredSkills, educationRequirement, minExperienceYears,
                maxExperienceYears, createdAt, createdAt, JobStatus.OPEN);
    }

    public Job {
        if (id == null) {
            throw new IllegalArgumentException("岗位 ID 不能为空");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("岗位名称不能为空");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("岗位描述不能为空");
        }
        if (minExperienceYears < 0 || maxExperienceYears < 0) {
            throw new IllegalArgumentException("工作年限不能为负数");
        }
        if (maxExperienceYears > 0 && minExperienceYears > maxExperienceYears) {
            throw new IllegalArgumentException("最低工作年限不能大于最高工作年限");
        }
        requiredSkills = normalize(requiredSkills);
        preferredSkills = normalize(preferredSkills);
        if (educationRequirement == null) {
            educationRequirement = EducationLevel.UNKNOWN;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (status == null) {
            status = JobStatus.OPEN;
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

    public Job update(String newName, String newDescription, Set<String> newRequiredSkills,
                      Set<String> newPreferredSkills, EducationLevel newEducationRequirement,
                      double newMinExperienceYears, double newMaxExperienceYears) {
        return new Job(id, newName, newDescription, newRequiredSkills, newPreferredSkills,
                newEducationRequirement, newMinExperienceYears, newMaxExperienceYears, createdAt, Instant.now(), status);
    }

    public Job close() {
        return withStatus(JobStatus.CLOSED);
    }

    public Job markDeleted() {
        return withStatus(JobStatus.DELETED);
    }

    private Job withStatus(JobStatus newStatus) {
        return new Job(id, name, description, requiredSkills, preferredSkills, educationRequirement,
                minExperienceYears, maxExperienceYears, createdAt, Instant.now(), newStatus);
    }
}
