package com.recruit.agent.job.infrastructure.persistence;

import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.domain.JobStatus;
import com.recruit.agent.common.json.StringSetJsonCodec;
import com.recruit.agent.screening.domain.EducationLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class JobPoConverter {

    private final StringSetJsonCodec jsonCodec = new StringSetJsonCodec();

    public JobPo toPo(Job job) {
        JobPo po = new JobPo();
        po.setId(job.id());
        po.setJobName(job.name());
        po.setDescription(job.description());
        po.setRequiredSkills(jsonCodec.encode(job.requiredSkills()));
        po.setPreferredSkills(jsonCodec.encode(job.preferredSkills()));
        po.setEducationRequirement(job.educationRequirement().name());
        po.setMinExperienceYears(BigDecimal.valueOf(job.minExperienceYears()));
        po.setMaxExperienceYears(BigDecimal.valueOf(job.maxExperienceYears()));
        po.setCreatedAt(LocalDateTime.ofInstant(job.createdAt(), ZoneOffset.UTC));
        po.setUpdatedAt(LocalDateTime.ofInstant(job.updatedAt(), ZoneOffset.UTC));
        po.setStatus(job.status().name());
        return po;
    }

    public Job toDomain(JobPo po) {
        return new Job(
                po.getId(),
                po.getJobName(),
                po.getDescription(),
                jsonCodec.decode(po.getRequiredSkills()),
                jsonCodec.decode(po.getPreferredSkills()),
                EducationLevel.valueOf(po.getEducationRequirement()),
                po.getMinExperienceYears().doubleValue(),
                po.getMaxExperienceYears().doubleValue(),
                po.getCreatedAt().toInstant(ZoneOffset.UTC),
                po.getUpdatedAt() == null ? po.getCreatedAt().toInstant(ZoneOffset.UTC) : po.getUpdatedAt().toInstant(ZoneOffset.UTC),
                po.getStatus() == null ? JobStatus.OPEN : JobStatus.valueOf(po.getStatus())
        );
    }
}
