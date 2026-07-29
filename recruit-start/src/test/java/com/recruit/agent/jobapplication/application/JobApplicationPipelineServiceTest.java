package com.recruit.agent.jobapplication.application;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.candidate.infrastructure.InMemoryCandidateRepository;
import com.recruit.agent.job.domain.Job;
import com.recruit.agent.job.infrastructure.InMemoryJobRepository;
import com.recruit.agent.jobapplication.domain.JobApplicationStage;
import com.recruit.agent.jobapplication.infrastructure.InMemoryJobApplicationRepository;
import com.recruit.agent.screening.domain.EducationLevel;
import com.recruit.agent.screening.domain.ScreeningRecommendation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JobApplicationPipelineServiceTest {

    private final InMemoryJobApplicationRepository applicationRepository = new InMemoryJobApplicationRepository();
    private final InMemoryJobRepository jobRepository = new InMemoryJobRepository();
    private final InMemoryCandidateRepository candidateRepository = new InMemoryCandidateRepository();
    private final JobApplicationPipelineService service = new JobApplicationPipelineService(
            applicationRepository, jobRepository, candidateRepository);

    @Test
    void createsApplicationAndRecordsInitialHistory() {
        seedJobAndCandidate(1L, 2L);

        var application = service.create(new CreateJobApplicationCommand(1L, 2L, 9L, "IMPORT", "重点候选人"));

        assertThat(application.stage()).isEqualTo(JobApplicationStage.APPLIED);
        assertThat(application.ownerUserId()).isEqualTo(9L);
        assertThat(service.history(application.id())).hasSize(1);
        assertThat(service.history(application.id()).get(0).toStage()).isEqualTo(JobApplicationStage.APPLIED);
    }

    @Test
    void screeningReusesSameApplicationAndMovesToScreening() {
        seedJobAndCandidate(1L, 2L);

        var first = service.markScreeningStarted(1L, 2L, 100L);
        var second = service.markScreeningStarted(1L, 2L, 101L);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.latestEvaluationId()).isEqualTo(101L);
        assertThat(second.stage()).isEqualTo(JobApplicationStage.SCREENING);
        assertThat(service.history(second.id()))
                .extracting("toStage")
                .contains(JobApplicationStage.APPLIED, JobApplicationStage.SCREENING);
    }

    @Test
    void rejectedHumanReviewClosesApplication() {
        seedJobAndCandidate(1L, 2L);

        var application = service.markHumanReviewCompleted(1L, 2L, 100L, "reject", ScreeningRecommendation.NOT_MATCH);

        assertThat(application.stage()).isEqualTo(JobApplicationStage.REJECTED);
        assertThat(application.closedAt()).isNotNull();
        assertThat(application.rejectionReason()).contains("reject");
    }

    @Test
    void conversationMovesApplicationToCommunication() {
        seedJobAndCandidate(1L, 2L);

        var application = service.attachConversation(1L, 2L, 800L);

        assertThat(application.stage()).isEqualTo(JobApplicationStage.COMMUNICATION);
        assertThat(application.latestConversationId()).isEqualTo(800L);
    }

    private void seedJobAndCandidate(Long jobId, Long candidateId) {
        jobRepository.save(new Job(jobId, "Java Backend", "Build recruiting agent backend",
                Set.of("java", "spring boot"), Set.of("mysql"), EducationLevel.BACHELOR,
                1, 5, Instant.now()));
        candidateRepository.save(new Candidate(candidateId, "Alice", null, null, EducationLevel.BACHELOR,
                3, "Shanghai", Set.of("java", "spring boot"), Instant.now()));
    }
}
