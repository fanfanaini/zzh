package com.recruit.agent.job.infrastructure.persistence;

import com.recruit.agent.job.domain.Job;
import com.recruit.agent.screening.domain.EducationLevel;

import java.time.Instant;
import java.util.Set;

public class JobPoConverterSpec {

    public static void main(String[] args) {
        shouldConvertBetweenDomainAndPo();
        System.out.println("JobPoConverterSpec passed");
    }

    private static void shouldConvertBetweenDomainAndPo() {
        JobPoConverter converter = new JobPoConverter();
        Job job = new Job(
                1L,
                "Java后端开发工程师",
                "负责招聘系统后端接口与AI任务编排",
                Set.of("java", "spring boot"),
                Set.of("redis"),
                EducationLevel.BACHELOR,
                1,
                3,
                Instant.parse("2026-07-11T00:00:00Z")
        );

        Job converted = converter.toDomain(converter.toPo(job));

        assertEquals(job.id(), converted.id(), "ID 应保持一致");
        assertEquals(job.requiredSkills(), converted.requiredSkills(), "必须技能应保持一致");
        assertEquals(job.educationRequirement(), converted.educationRequirement(), "学历要求应保持一致");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ", expected=" + expected + ", actual=" + actual);
        }
    }
}
