package com.recruit.agent.screening.infrastructure.persistence;

import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.ScreeningRecommendation;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationPoConverterTest {

    private final EvaluationPoConverter converter = new EvaluationPoConverter();

    @Test
    void convertsEvaluationToPoAndBack() {
        Evaluation evaluation = new Evaluation(
                9001L,
                1001L,
                2001L,
                92.5,
                null,
                92.5,
                ScreeningRecommendation.INTERVIEW,
                0.925,
                List.of("required skills matched"),
                List.of("salary expectation unknown"),
                List.of("portfolio missing"),
                null,
                null,
                null,
                Instant.parse("2026-07-11T00:00:00Z")
        );

        EvaluationPo po = converter.toPo(evaluation);
        Evaluation restored = converter.toDomain(po);

        assertThat(po.getJobId()).isEqualTo(1001L);
        assertThat(po.getAdvantages()).isEqualTo("[\"required skills matched\"]");
        assertThat(restored).isEqualTo(evaluation);
    }
}
