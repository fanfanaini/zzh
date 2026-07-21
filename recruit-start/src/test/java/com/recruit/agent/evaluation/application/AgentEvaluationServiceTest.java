package com.recruit.agent.evaluation.application;

import com.recruit.agent.evaluation.domain.EvaluationCase;
import com.recruit.agent.evaluation.domain.EvaluationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class AgentEvaluationServiceTest {

    private final AgentEvaluationService service = new AgentEvaluationService();

    @Test
    void calculatesDeterministicOfflineMetrics() {
        List<EvaluationCase> cases = List.of(
                evaluationCase("c1", "A", "R1", "NONE", List.of("d1", "d2"), true, false),
                evaluationCase("c2", "A", "R2", "schedule", List.of("d3"), true, false),
                evaluationCase("c3", "B", "R3", "NONE", List.of(), true, true),
                evaluationCase("c4", "B", "R3", "NONE", List.of(), false, true)
        );
        List<EvaluationResult> results = List.of(
                result("c1", "A", "R1", "NONE", List.of("d2", "x", "d1"), true, false, 100L, 10, 5, null, null),
                result("c2", "B", "WRONG", "NONE", List.of("x", "d3"), true, true, 200L, 20, 10, null, null),
                result("c3", "B", "R3", "NONE", List.of(), false, true, 300L, null, null, "TOOL", "TIMEOUT"),
                result("c4", "B", "R3", "NONE", List.of(), false, false, null, 40, 20, "TOOL", "TIMEOUT")
        );

        var report = service.evaluate(cases, results, 2);

        assertThat(report.intent().accuracy()).isCloseTo(0.75, within(0.0001));
        assertThat(report.intent().macroF1()).isCloseTo(0.733333, within(0.0001));
        assertThat(report.routing().exactMatchRate()).isCloseTo(0.75, within(0.0001));
        assertThat(report.toolSelection().exactMatchRate()).isCloseTo(0.75, within(0.0001));
        assertThat(report.retrieval().recallAtK()).isCloseTo(0.75, within(0.0001));
        assertThat(report.retrieval().meanReciprocalRank()).isCloseTo(0.75, within(0.0001));
        assertThat(report.endToEnd().successRate()).isCloseTo(0.5, within(0.0001));
        assertThat(report.endToEnd().expectedOutcomeMatchRate()).isCloseTo(0.75, within(0.0001));
        assertThat(report.handoff().precision()).isCloseTo(0.5, within(0.0001));
        assertThat(report.handoff().recall()).isCloseTo(0.5, within(0.0001));
        assertThat(report.handoff().f1()).isCloseTo(0.5, within(0.0001));
        assertThat(report.runtime().latencySamples()).isEqualTo(3);
        assertThat(report.runtime().averageLatencyMillis()).isEqualTo(200.0);
        assertThat(report.runtime().p50LatencyMillis()).isEqualTo(200.0);
        assertThat(report.runtime().p95LatencyMillis()).isEqualTo(290.0);
        assertThat(report.runtime().promptTokens()).isEqualTo(70);
        assertThat(report.runtime().completionTokens()).isEqualTo(35);
        assertThat(report.failureCounts()).containsEntry("TOOL:TIMEOUT", 2L);
    }

    @Test
    void rejectsMissingAndCrossVersionResults() {
        EvaluationCase onlyCase = evaluationCase("c1", "A", "R1", "NONE", List.of(), true, false);

        assertThatThrownBy(() -> service.evaluate(List.of(onlyCase), List.of(
                result("c2", "A", "R1", "NONE", List.of(), true, false, 1L, 1, 1, null, null)), 5))
                .hasMessageContaining("missing result for caseId: c1");

        EvaluationResult wrongVersion = new EvaluationResult(EvaluationResult.SCHEMA_VERSION, "other", "c1",
                "A", "R1", "NONE", List.of(), true, false, 1L, 1, 1, null, null);
        assertThatThrownBy(() -> service.evaluate(List.of(onlyCase), List.of(wrongVersion), 5))
                .hasMessageContaining("matching datasetVersion");
    }

    private EvaluationCase evaluationCase(String id, String intent, String route, String tool,
                                            List<String> relevant, boolean success, boolean handoff) {
        return new EvaluationCase(EvaluationCase.SCHEMA_VERSION, "dataset/v1", id, "curated:test", "message",
                intent, route, tool, relevant, success, handoff, List.of("test"));
    }

    private EvaluationResult result(String id, String intent, String route, String tool, List<String> retrieved,
                                    boolean success, boolean handoff, Long latency, Integer promptTokens,
                                    Integer completionTokens, String errorComponent, String errorCode) {
        return new EvaluationResult(EvaluationResult.SCHEMA_VERSION, "dataset/v1", id, intent, route, tool,
                retrieved, success, handoff, latency, promptTokens, completionTokens, errorComponent, errorCode);
    }
}
