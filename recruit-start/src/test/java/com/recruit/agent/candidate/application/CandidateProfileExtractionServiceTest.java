package com.recruit.agent.candidate.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.domain.ResumeDocument;
import com.recruit.agent.candidate.domain.ResumeParseStatus;
import com.recruit.agent.candidate.domain.DetailedResumeProfileExtractor;
import com.recruit.agent.candidate.domain.ResumeProfileExtractionResult;
import com.recruit.agent.candidate.infrastructure.InMemoryCandidateRepository;
import com.recruit.agent.candidate.infrastructure.InMemoryResumeRepository;
import com.recruit.agent.candidate.infrastructure.RuleBasedResumeProfileExtractor;
import com.recruit.agent.screening.domain.EducationLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateProfileExtractionServiceTest {

    @Test
    void extractsProfileAndUpdatesCandidate() {
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        InMemoryResumeRepository resumeRepository = new InMemoryResumeRepository();
        Candidate candidate = candidateRepository.save(new Candidate(
                candidateRepository.nextCandidateId(),
                "张三",
                "phone-hash",
                "email-hash",
                EducationLevel.UNKNOWN,
                0,
                null,
                Set.of(),
                Instant.parse("2026-07-11T00:00:00Z")
        ));
        ResumeDocument resume = resumeRepository.save(new ResumeDocument(
                resumeRepository.nextResumeId(),
                candidate.id(),
                "resume.txt",
                "text/plain",
                100,
                "resume.txt",
                ResumeParseStatus.PARSED,
                "本科 成都 3年 Java Spring Boot MySQL Redis",
                Instant.parse("2026-07-11T00:00:00Z")
        ));
        CandidateProfileExtractionService service = new CandidateProfileExtractionService(
                candidateRepository,
                resumeRepository,
                new RuleBasedResumeProfileExtractor()
        );

        Candidate updated = service.extractProfile(resume.id());

        assertThat(updated.educationLevel()).isEqualTo(EducationLevel.BACHELOR);
        assertThat(updated.totalExperienceYears()).isEqualTo(3.0);
        assertThat(updated.expectedCity()).isEqualTo("成都");
        assertThat(updated.skills()).contains("java", "spring boot", "mysql", "redis");
        assertThat(candidateRepository.findById(candidate.id()).orElseThrow().skills()).contains("java");
    }

    @Test
    void rejectsResumeWithoutRawText() {
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        InMemoryResumeRepository resumeRepository = new InMemoryResumeRepository();
        ResumeDocument resume = resumeRepository.save(new ResumeDocument(
                resumeRepository.nextResumeId(),
                2001L,
                "resume.txt",
                "text/plain",
                100,
                "resume.txt",
                ResumeParseStatus.UPLOADED,
                null,
                Instant.parse("2026-07-11T00:00:00Z")
        ));
        CandidateProfileExtractionService service = new CandidateProfileExtractionService(
                candidateRepository,
                resumeRepository,
                new RuleBasedResumeProfileExtractor()
        );

        assertThatThrownBy(() -> service.extractProfile(resume.id()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("尚未完成文本提取");
    }

    @Test
    void persistsStructuredDetailsWhenDetailedExtractorIsAvailable() {
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        InMemoryResumeRepository resumeRepository = new InMemoryResumeRepository();
        Candidate candidate = candidateRepository.save(new Candidate(
                candidateRepository.nextCandidateId(), "李四", null, null, EducationLevel.UNKNOWN,
                0, null, Set.of(), Instant.now()));
        ResumeDocument resume = resumeRepository.save(new ResumeDocument(
                resumeRepository.nextResumeId(), candidate.id(), "resume.txt", "text/plain", 100,
                "resume.txt", ResumeParseStatus.PARSED, "示例大学 本科 Java", Instant.now()));
        DetailedResumeProfileExtractor extractor = rawText -> new ResumeProfileExtractionResult(
                new com.recruit.agent.candidate.domain.CandidateProfileDraft(
                        EducationLevel.BACHELOR, 1, "成都", Set.of("java")),
                new ResumeProfileExtractionResult.StructuredDetail(
                        List.of(new ResumeProfileExtractionResult.EducationExperience(
                                "示例大学", "本科", "软件工程", null, null, "示例大学 本科", 0.95)),
                        List.of(), List.of(), List.of(), List.of(), List.of(),
                        Map.of("educationLevel", "本科"), Map.of("educationLevel", 0.95))
        );
        CandidateProfileExtractionService service = new CandidateProfileExtractionService(
                candidateRepository, resumeRepository, extractor);

        service.extractProfile(resume.id());

        assertThat(resumeRepository.findById(resume.id()).orElseThrow().structuredDetailJson())
                .contains("示例大学")
                .contains("fieldConfidence");
    }
}
