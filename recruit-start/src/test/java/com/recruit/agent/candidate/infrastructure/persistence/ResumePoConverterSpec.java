package com.recruit.agent.candidate.infrastructure.persistence;

import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;

import java.time.Instant;

public class ResumePoConverterSpec {

    public static void main(String[] args) {
        shouldConvertBetweenDomainAndPo();
        System.out.println("ResumePoConverterSpec passed");
    }

    private static void shouldConvertBetweenDomainAndPo() {
        ResumePoConverter converter = new ResumePoConverter();
        ResumeDocument resume = new ResumeDocument(
                10L,
                1L,
                "resume.pdf",
                "application/pdf",
                1024,
                "resumes/2026/resume.pdf",
                ResumeParseStatus.PARSED,
                "Java Spring Boot 项目经验",
                Instant.parse("2026-07-11T00:00:00Z")
        );

        ResumeDocument converted = converter.toDomain(converter.toPo(resume));

        assertEquals(resume.id(), converted.id(), "ID 应保持一致");
        assertEquals(resume.parseStatus(), converted.parseStatus(), "解析状态应保持一致");
        assertEquals(resume.rawText(), converted.rawText(), "原始文本应保持一致");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ", expected=" + expected + ", actual=" + actual);
        }
    }
}
