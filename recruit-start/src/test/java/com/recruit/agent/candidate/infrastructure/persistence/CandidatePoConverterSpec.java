package com.recruit.agent.candidate.infrastructure.persistence;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.screening.domain.EducationLevel;

import java.time.Instant;
import java.util.Set;

public class CandidatePoConverterSpec {

    public static void main(String[] args) {
        shouldConvertBetweenDomainAndPo();
        System.out.println("CandidatePoConverterSpec passed");
    }

    private static void shouldConvertBetweenDomainAndPo() {
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

        assertEquals(candidate.id(), converted.id(), "ID 应保持一致");
        assertEquals(candidate.skills(), converted.skills(), "技能集合应保持一致");
        assertEquals(candidate.educationLevel(), converted.educationLevel(), "学历应保持一致");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ", expected=" + expected + ", actual=" + actual);
        }
    }
}
