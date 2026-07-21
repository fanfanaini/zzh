package com.recruit.agent.candidate.infrastructure.persistence;

import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ResumePoConverterTest {

    @Test
    void convertsBetweenDomainAndPo() {
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

        assertThat(converted.id()).isEqualTo(resume.id());
        assertThat(converted.parseStatus()).isEqualTo(resume.parseStatus());
        assertThat(converted.rawText()).isEqualTo(resume.rawText());
    }
}
