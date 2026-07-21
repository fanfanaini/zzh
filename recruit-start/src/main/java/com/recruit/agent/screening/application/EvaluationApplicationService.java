package com.recruit.agent.screening.application;

import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.EvaluationRepository;

import java.util.List;
import java.util.NoSuchElementException;

public class EvaluationApplicationService {

    private final EvaluationRepository repository;

    public EvaluationApplicationService(EvaluationRepository repository) {
        this.repository = repository;
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
        return repository.save(evaluation);
    }
}
