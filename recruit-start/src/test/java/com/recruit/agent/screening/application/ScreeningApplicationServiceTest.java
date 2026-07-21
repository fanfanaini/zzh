package com.recruit.agent.screening.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.infrastructure.InMemoryCandidateRepository;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.infrastructure.InMemoryJobRepository;
import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.ResumeScreeningService;
import com.recruit.agent.screening.domain.ScreeningReviewRequest;
import com.recruit.agent.screening.domain.ScreeningReviewService;
import com.recruit.agent.screening.domain.ScreeningRecommendation;
import com.recruit.agent.screening.domain.ScreeningResult;
import com.recruit.agent.screening.domain.EducationLevel;
import com.recruit.agent.screening.infrastructure.InMemoryEvaluationRepository;
import com.recruit.agent.vector.application.VectorApplicationService;
import com.recruit.agent.vector.infrastructure.DeterministicEmbeddingService;
import com.recruit.agent.vector.infrastructure.InMemoryVectorSearchRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScreeningApplicationServiceTest {

    @Test
    void screensCandidateForJob() {
        InMemoryJobRepository jobRepository = new InMemoryJobRepository();
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        InMemoryEvaluationRepository evaluationRepository = new InMemoryEvaluationRepository();
        Job job = jobRepository.save(javaJob(jobRepository.nextId()));
        Candidate candidate = candidateRepository.save(javaCandidate(candidateRepository.nextCandidateId()));
        ScreeningApplicationService service = new ScreeningApplicationService(
                jobRepository,
                candidateRepository,
                new ResumeScreeningService(),
                evaluationRepository,
                Optional.empty(),
                Optional.empty()
        );

        ScreeningResult result = service.screen(new ScreenCandidateCommand(job.id(), candidate.id()));

        assertThat(result.recommendation()).isEqualTo(ScreeningRecommendation.INTERVIEW);
        assertThat(result.score().finalScore()).isGreaterThanOrEqualTo(80);
        Evaluation evaluation = evaluationRepository.findByJobIdAndCandidateId(job.id(), candidate.id()).get(0);
        assertThat(evaluation.finalScore()).isEqualTo(result.score().finalScore());
        assertThat(evaluation.recommendation()).isEqualTo(result.recommendation());
    }

    @Test
    void rejectsMissingCandidate() {
        InMemoryJobRepository jobRepository = new InMemoryJobRepository();
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        Job job = jobRepository.save(javaJob(jobRepository.nextId()));
        ScreeningApplicationService service = new ScreeningApplicationService(
                jobRepository,
                candidateRepository,
                new ResumeScreeningService(),
                new InMemoryEvaluationRepository(),
                Optional.empty(),
                Optional.empty()
        );

        assertThatThrownBy(() -> service.screen(new ScreenCandidateCommand(job.id(), 404L)))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("候选人不存在");
    }

    @Test
    void usesVectorSimilarityAsSemanticScoreWhenAvailable() {
        InMemoryJobRepository jobRepository = new InMemoryJobRepository();
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        InMemoryEvaluationRepository evaluationRepository = new InMemoryEvaluationRepository();
        InMemoryVectorSearchRepository vectorRepository = new InMemoryVectorSearchRepository();
        VectorApplicationService vectorService = new VectorApplicationService(
                new DeterministicEmbeddingService(),
                vectorRepository
        );
        Job job = jobRepository.save(javaJob(jobRepository.nextId()));
        Candidate candidate = candidateRepository.save(javaCandidate(candidateRepository.nextCandidateId()));
        vectorService.indexResumeProject(
                candidate.id(),
                1L,
                "负责招聘系统后端接口开发，使用 Java Spring Boot MySQL Redis 完成 AI 任务编排"
        );
        ScreeningApplicationService service = new ScreeningApplicationService(
                jobRepository,
                candidateRepository,
                new ResumeScreeningService(),
                evaluationRepository,
                Optional.of(vectorService),
                Optional.empty()
        );

        ScreeningResult result = service.screen(new ScreenCandidateCommand(job.id(), candidate.id()));

        Evaluation evaluation = evaluationRepository.findByJobIdAndCandidateId(job.id(), candidate.id()).get(0);
        assertThat(result.score().projectScore()).isGreaterThan(0);
        assertThat(evaluation.semanticScore()).isEqualTo(result.score().projectScore());
    }

    @Test
    void storesAiReviewedScreeningEvidence() {
        InMemoryJobRepository jobRepository = new InMemoryJobRepository();
        InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
        InMemoryEvaluationRepository evaluationRepository = new InMemoryEvaluationRepository();
        Job job = jobRepository.save(javaJob(jobRepository.nextId()));
        Candidate candidate = candidateRepository.save(javaCandidate(candidateRepository.nextCandidateId()));
        ScreeningReviewService reviewService = request -> new ScreeningResult(
                request.ruleResult().recommendation(),
                request.ruleResult().score(),
                List.of("AI复核: 项目经历与岗位职责有明确对应"),
                List.of("AI复核: 未发现高并发压测证据"),
                List.of("建议追问: Redis 缓存一致性如何处理？")
        );
        ScreeningApplicationService service = new ScreeningApplicationService(
                jobRepository,
                candidateRepository,
                new ResumeScreeningService(),
                evaluationRepository,
                Optional.empty(),
                Optional.of(reviewService)
        );

        ScreeningResult result = service.screen(new ScreenCandidateCommand(job.id(), candidate.id()));

        Evaluation evaluation = evaluationRepository.findByJobIdAndCandidateId(job.id(), candidate.id()).get(0);
        assertThat(result.advantages()).contains("AI复核: 项目经历与岗位职责有明确对应");
        assertThat(evaluation.risks()).contains("AI复核: 未发现高并发压测证据");
        assertThat(evaluation.missingInformation()).contains("建议追问: Redis 缓存一致性如何处理？");
    }

    private Job javaJob(Long id) {
        return new Job(
                id,
                "Java后端开发工程师",
                "负责招聘系统后端接口与AI任务编排",
                Set.of("java", "spring boot", "mysql"),
                Set.of("redis"),
                EducationLevel.BACHELOR,
                1,
                3,
                Instant.parse("2026-07-11T00:00:00Z")
        );
    }

    private Candidate javaCandidate(Long id) {
        return new Candidate(
                id,
                "张三",
                "phone-hash",
                "email-hash",
                EducationLevel.BACHELOR,
                3,
                "成都",
                Set.of("java", "spring boot", "mysql", "redis"),
                Instant.parse("2026-07-11T00:00:00Z")
        );
    }
}
