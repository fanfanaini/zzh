package com.recruit.agent.job.api;

import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.domain.JobStatus;
import com.recruit.agent.screening.domain.EducationLevel;

import java.time.Instant;
import java.util.Set;

public record JobResponse(
        Long id,
        String name,
        String description,
        Set<String> requiredSkills,
        Set<String> preferredSkills,
        EducationLevel educationRequirement,
        double minExperienceYears,
        double maxExperienceYears,
        JobStatus status,
        Instant updatedAt,
        Instant createdAt
) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.id(),
                job.name(),
                job.description(),
                job.requiredSkills(),
                job.preferredSkills(),
                job.educationRequirement(),
                job.minExperienceYears(),
                job.maxExperienceYears(),
                job.status(),
                job.updatedAt(),
                job.createdAt()
        );
    }
}
