package com.recruit.agent.candidate.infrastructure.persistence;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.CandidateStatus;
import com.recruit.agent.common.json.StringSetJsonCodec;
import com.recruit.agent.screening.domain.EducationLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class CandidatePoConverter {

    private final StringSetJsonCodec jsonCodec = new StringSetJsonCodec();

    public CandidatePo toPo(Candidate candidate) {
        CandidatePo po = new CandidatePo();
        po.setId(candidate.id());
        po.setName(candidate.name());
        po.setPhoneHash(candidate.phoneHash());
        po.setEmailHash(candidate.emailHash());
        po.setEducationLevel(candidate.educationLevel().name());
        po.setTotalExperienceYears(BigDecimal.valueOf(candidate.totalExperienceYears()));
        po.setExpectedCity(candidate.expectedCity());
        po.setSkills(jsonCodec.encode(candidate.skills()));
        po.setCreatedAt(LocalDateTime.ofInstant(candidate.createdAt(), ZoneOffset.UTC));
        po.setUpdatedAt(LocalDateTime.ofInstant(candidate.updatedAt(), ZoneOffset.UTC));
        po.setStatus(candidate.status().name());
        return po;
    }

    public Candidate toDomain(CandidatePo po) {
        return new Candidate(
                po.getId(),
                po.getName(),
                po.getPhoneHash(),
                po.getEmailHash(),
                EducationLevel.valueOf(po.getEducationLevel()),
                po.getTotalExperienceYears().doubleValue(),
                po.getExpectedCity(),
                jsonCodec.decode(po.getSkills()),
                po.getCreatedAt().toInstant(ZoneOffset.UTC),
                po.getUpdatedAt() == null ? po.getCreatedAt().toInstant(ZoneOffset.UTC) : po.getUpdatedAt().toInstant(ZoneOffset.UTC),
                po.getStatus() == null ? CandidateStatus.ACTIVE : CandidateStatus.valueOf(po.getStatus())
        );
    }
}
