package com.recruit.agent.jobapplication.application;

import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.jobapplication.domain.JobApplication;
import com.recruit.agent.jobapplication.domain.JobApplicationHistory;
import com.recruit.agent.jobapplication.domain.JobApplicationRepository;
import com.recruit.agent.jobapplication.domain.JobApplicationStage;
import com.recruit.agent.screening.domain.ScreeningRecommendation;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

public class JobApplicationPipelineService {

    private final JobApplicationRepository repository;
    private final JobRepository jobRepository;
    private final CandidateRepository candidateRepository;

    public JobApplicationPipelineService(JobApplicationRepository repository, JobRepository jobRepository,
                                         CandidateRepository candidateRepository) {
        this.repository = repository;
        this.jobRepository = jobRepository;
        this.candidateRepository = candidateRepository;
    }

    public JobApplication create(CreateJobApplicationCommand command) {
        requireJob(command.jobId());
        requireCandidate(command.candidateId());
        return repository.findByJobIdAndCandidateId(command.jobId(), command.candidateId())
                .orElseGet(() -> {
                    JobApplication application = new JobApplication(
                            repository.nextApplicationId(),
                            command.jobId(),
                            command.candidateId(),
                            command.ownerUserId(),
                            command.source(),
                            command.note(),
                            Instant.now()
                    );
                    JobApplication saved = repository.save(application);
                    appendHistory(saved.id(), null, saved.stage(), command.ownerUserId(), "application created");
                    return saved;
                });
    }

    public JobApplication get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("投递记录不存在: " + id));
    }

    public List<JobApplication> list(Long jobId, Long candidateId, JobApplicationStage stage) {
        return repository.findAll().stream()
                .filter(application -> jobId == null || application.jobId().equals(jobId))
                .filter(application -> candidateId == null || application.candidateId().equals(candidateId))
                .filter(application -> stage == null || application.stage() == stage)
                .sorted(Comparator.comparing(JobApplication::updatedAt).reversed())
                .toList();
    }

    public List<JobApplicationHistory> history(Long applicationId) {
        get(applicationId);
        return repository.findHistoryByApplicationId(applicationId);
    }

    public JobApplication advance(AdvanceJobApplicationCommand command) {
        JobApplication existing = get(command.applicationId());
        JobApplication updated = existing.moveTo(command.stage(), command.operatorUserId(), command.reason(),
                command.evaluationId(), command.conversationId(), command.note());
        JobApplication saved = repository.save(updated);
        if (existing.stage() != saved.stage()) {
            appendHistory(saved.id(), existing.stage(), saved.stage(), command.operatorUserId(), command.reason());
        }
        return saved;
    }

    public JobApplication markScreeningStarted(Long jobId, Long candidateId, Long evaluationId) {
        JobApplication application = create(new CreateJobApplicationCommand(jobId, candidateId, null,
                "SCREENING", null));
        if (application.stage().terminal()) {
            return repository.save(application.attachEvaluation(evaluationId));
        }
        return advance(new AdvanceJobApplicationCommand(application.id(), JobApplicationStage.SCREENING, null,
                "screening completed", evaluationId, null, null));
    }

    public JobApplication markHumanReviewCompleted(Long jobId, Long candidateId, Long evaluationId,
                                                   String humanResult, ScreeningRecommendation recommendation) {
        JobApplication application = create(new CreateJobApplicationCommand(jobId, candidateId, null,
                "HUMAN_REVIEW", null));
        JobApplicationStage nextStage = rejected(humanResult, recommendation)
                ? JobApplicationStage.REJECTED
                : JobApplicationStage.COMMUNICATION;
        String reason = humanResult == null || humanResult.isBlank()
                ? "human review completed"
                : "human review: " + humanResult.trim();
        return advance(new AdvanceJobApplicationCommand(application.id(), nextStage, null, reason,
                evaluationId, null, null));
    }

    public JobApplication attachConversation(Long jobId, Long candidateId, Long conversationId) {
        JobApplication application = create(new CreateJobApplicationCommand(jobId, candidateId, null,
                "CONVERSATION", null));
        if (application.stage().terminal()) {
            return repository.save(application.attachConversation(conversationId));
        }
        return advance(new AdvanceJobApplicationCommand(application.id(), JobApplicationStage.COMMUNICATION, null,
                "conversation created", null, conversationId, null));
    }

    private boolean rejected(String humanResult, ScreeningRecommendation recommendation) {
        String normalized = humanResult == null ? "" : humanResult.trim().toUpperCase();
        return normalized.contains("REJECT")
                || normalized.contains("FAIL")
                || normalized.contains("不通过")
                || recommendation == ScreeningRecommendation.NOT_MATCH;
    }

    private void appendHistory(Long applicationId, JobApplicationStage fromStage, JobApplicationStage toStage,
                               Long operatorUserId, String reason) {
        repository.saveHistory(new JobApplicationHistory(repository.nextHistoryId(), applicationId, fromStage, toStage,
                operatorUserId, reason, Instant.now()));
    }

    private void requireJob(Long jobId) {
        if (jobRepository.findById(jobId).isEmpty()) {
            throw new NoSuchElementException("岗位不存在: " + jobId);
        }
    }

    private void requireCandidate(Long candidateId) {
        if (candidateRepository.findById(candidateId).isEmpty()) {
            throw new NoSuchElementException("候选人不存在: " + candidateId);
        }
    }
}
