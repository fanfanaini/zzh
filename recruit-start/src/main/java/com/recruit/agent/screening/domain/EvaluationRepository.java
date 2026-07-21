package com.recruit.agent.screening.domain;

import java.util.Optional;
import java.util.List;

public interface EvaluationRepository {

    Evaluation save(Evaluation evaluation);

    Optional<Evaluation> findById(Long id);

    List<Evaluation> findByJobIdAndCandidateId(Long jobId, Long candidateId);

    List<Evaluation> findLatestByJobId(Long jobId);

    long nextEvaluationId();
}
