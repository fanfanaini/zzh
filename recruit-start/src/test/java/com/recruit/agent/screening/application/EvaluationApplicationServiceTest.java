package com.recruit.agent.screening.application;

import com.recruit.agent.screening.domain.Evaluation;
import com.recruit.agent.screening.domain.ScreeningRecommendation;
import com.recruit.agent.screening.infrastructure.InMemoryEvaluationRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationApplicationServiceTest {

    @Test
    void savesHumanReviewResult() {
        InMemoryEvaluationRepository repository = new InMemoryEvaluationRepository();
        Evaluation evaluation = repository.save(new Evaluation(
                repository.nextEvaluationId(),
                1001L,
                2001L,
                88.0,
                null,
                88.0,
                ScreeningRecommendation.INTERVIEW,
                0.88,
                List.of("技能匹配"),
                List.of(),
                List.of(),
                null,
                null,
                null,
                Instant.parse("2026-07-11T00:00:00Z")
        ));
        EvaluationApplicationService service = new EvaluationApplicationService(repository);

        Evaluation reviewed = service.humanReview(evaluation.id(), "accepted");

        assertThat(reviewed.humanResult()).isEqualTo("ACCEPTED");
        assertThat(service.get(evaluation.id()).humanResult()).isEqualTo("ACCEPTED");
    }
}
