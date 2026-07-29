package com.recruit.agent.screening.application;

import com.recruit.agent.jobapplication.application.JobApplicationPipelineService;
import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.EvaluationRepository;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class EvaluationApplicationService {

    private final EvaluationRepository repository;
    private final Optional<JobApplicationPipelineService> jobApplicationPipelineService;

    public EvaluationApplicationService(EvaluationRepository repository) {
        this(repository, Optional.empty());
    }

    public EvaluationApplicationService(EvaluationRepository repository,
                                        Optional<JobApplicationPipelineService> jobApplicationPipelineService) {
        this.repository = repository;
        this.jobApplicationPipelineService = jobApplicationPipelineService;
    }

    public Evaluation get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("评估记录不存在: " + id));
    }

    public List<Evaluation> listByJobAndCandidate(Long jobId, Long candidateId) {
        return repository.findByJobIdAndCandidateId(jobId, candidateId);
    }

    public Evaluation humanReview(Long id, String humanResult) {
        Evaluation evaluation = get(id).withHumanResult(humanResult);
        Evaluation saved = repository.save(evaluation);
        jobApplicationPipelineService.ifPresent(service ->
                service.markHumanReviewCompleted(saved.jobId(), saved.candidateId(), saved.id(),
                        saved.humanResult(), saved.recommendation()));
        return saved;
    }
}
