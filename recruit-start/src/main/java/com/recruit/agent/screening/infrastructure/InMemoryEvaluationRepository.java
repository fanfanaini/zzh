package com.recruit.agent.screening.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.EvaluationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class InMemoryEvaluationRepository implements EvaluationRepository {

    private final Map<Long, Evaluation> evaluations = new ConcurrentHashMap<>();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    @Override
    public Evaluation save(Evaluation evaluation) {
        evaluations.put(evaluation.id(), evaluation);
        return evaluation;
    }

    @Override
    public Optional<Evaluation> findById(Long id) {
        return Optional.ofNullable(evaluations.get(id));
    }

    @Override
    public List<Evaluation> findByJobIdAndCandidateId(Long jobId, Long candidateId) {
        return evaluations.values().stream()
                .filter(evaluation -> evaluation.jobId().equals(jobId))
                .filter(evaluation -> evaluation.candidateId().equals(candidateId))
                .sorted(java.util.Comparator.comparing(Evaluation::createdAt).reversed())
                .toList();
    }

    @Override
    public List<Evaluation> findLatestByJobId(Long jobId) {
        return evaluations.values().stream()
                .filter(evaluation -> evaluation.jobId().equals(jobId))
                .collect(java.util.stream.Collectors.toMap(Evaluation::candidateId, evaluation -> evaluation,
                        (left, right) -> left.createdAt().isAfter(right.createdAt()) ? left : right))
                .values().stream()
                .sorted(java.util.Comparator.comparing(Evaluation::finalScore,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public long nextEvaluationId() {
        return idGenerator.nextId();
    }
}
