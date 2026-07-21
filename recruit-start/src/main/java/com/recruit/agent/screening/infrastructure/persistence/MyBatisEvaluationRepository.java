package com.recruit.agent.screening.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.EvaluationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisEvaluationRepository implements EvaluationRepository {

    private final EvaluationMapper mapper;
    private final EvaluationPoConverter converter = new EvaluationPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisEvaluationRepository(EvaluationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Evaluation save(Evaluation evaluation) {
        if (mapper.selectById(evaluation.id()) == null) {
            mapper.insert(converter.toPo(evaluation));
        } else {
            mapper.updateById(converter.toPo(evaluation));
        }
        return evaluation;
    }

    @Override
    public Optional<Evaluation> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id))
                .map(converter::toDomain);
    }

    @Override
    public List<Evaluation> findByJobIdAndCandidateId(Long jobId, Long candidateId) {
        return mapper.selectList(Wrappers.<EvaluationPo>lambdaQuery()
                        .eq(EvaluationPo::getJobId, jobId)
                        .eq(EvaluationPo::getCandidateId, candidateId)
                        .orderByDesc(EvaluationPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .toList();
    }

    @Override
    public List<Evaluation> findLatestByJobId(Long jobId) {
        return mapper.selectList(Wrappers.<EvaluationPo>lambdaQuery()
                        .eq(EvaluationPo::getJobId, jobId)
                        .orderByDesc(EvaluationPo::getCreatedAt))
                .stream()
                .map(converter::toDomain)
                .collect(java.util.stream.Collectors.toMap(Evaluation::candidateId, evaluation -> evaluation,
                        (newest, ignored) -> newest, java.util.LinkedHashMap::new))
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
