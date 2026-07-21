package com.recruit.agent.screening.infrastructure.persistence;

import com.recruit.agent.common.json.StringListJsonCodec;
import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.ScreeningRecommendation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class EvaluationPoConverter {

    private final StringListJsonCodec jsonCodec = new StringListJsonCodec();

    public EvaluationPo toPo(Evaluation evaluation) {
        EvaluationPo po = new EvaluationPo();
        po.setId(evaluation.id());
        po.setJobId(evaluation.jobId());
        po.setCandidateId(evaluation.candidateId());
        po.setRuleScore(toBigDecimal(evaluation.ruleScore()));
        po.setSemanticScore(toBigDecimal(evaluation.semanticScore()));
        po.setFinalScore(toBigDecimal(evaluation.finalScore()));
        po.setRecommendation(evaluation.recommendation().name());
        po.setConfidence(toBigDecimal(evaluation.confidence()));
        po.setAdvantages(jsonCodec.encode(evaluation.advantages()));
        po.setRisks(jsonCodec.encode(evaluation.risks()));
        po.setMissingInformation(jsonCodec.encode(evaluation.missingInformation()));
        po.setModelName(evaluation.modelName());
        po.setPromptVersion(evaluation.promptVersion());
        po.setHumanResult(evaluation.humanResult());
        po.setCreatedAt(LocalDateTime.ofInstant(evaluation.createdAt(), ZoneOffset.UTC));
        return po;
    }

    public Evaluation toDomain(EvaluationPo po) {
        return new Evaluation(
                po.getId(),
                po.getJobId(),
                po.getCandidateId(),
                toDouble(po.getRuleScore()),
                toDouble(po.getSemanticScore()),
                toDouble(po.getFinalScore()),
                ScreeningRecommendation.valueOf(po.getRecommendation()),
                toDouble(po.getConfidence()),
                jsonCodec.decode(po.getAdvantages()),
                jsonCodec.decode(po.getRisks()),
                jsonCodec.decode(po.getMissingInformation()),
                po.getModelName(),
                po.getPromptVersion(),
                po.getHumanResult(),
                po.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }

    private BigDecimal toBigDecimal(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value);
    }

    private Double toDouble(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.doubleValue();
    }
}
