package com.recruit.agent.candidate.infrastructure.persistence;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CandidatePoConverterTest {

    @Test
    void convertsBetweenDomainAndPo() {
        CandidatePoConverter converter = new CandidatePoConverter();
        Candidate candidate = new Candidate(
                1L,
                "王五",
                "phone-hash",
                "email-hash",
                EducationLevel.BACHELOR,
                2.5,
                "成都",
                Set.of("java", "spring boot"),
                Instant.parse("2026-07-11T00:00:00Z")
        );

        Candidate converted = converter.toDomain(converter.toPo(candidate));

        assertThat(converted.id()).isEqualTo(candidate.id());
        assertThat(converted.skills()).isEqualTo(candidate.skills());
        assertThat(converted.educationLevel()).isEqualTo(candidate.educationLevel());
    }
}
