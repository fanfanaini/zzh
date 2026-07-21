package com.recruit.agent.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OfflineEvaluationRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesAndScoresCapturedResultsWithoutLiveDependencies() throws Exception {
        Path cases = tempDir.resolve("cases.jsonl");
        Path results = tempDir.resolve("results.jsonl");
        Files.writeString(cases, """
                {"schemaVersion":"agent-evaluation-case/v1","datasetVersion":"demo/v1","caseId":"c1","provenance":"curated:test","message":"岗位职责是什么","expectedIntent":"JOB_INFO","expectedRoute":"KNOWLEDGE","expectedTool":"NONE","relevantDocumentIds":["job-1"],"expectedTaskSuccess":true,"expectedHandoff":false}
                """);
        Files.writeString(results, """
                {"schemaVersion":"agent-evaluation-result/v1","datasetVersion":"demo/v1","caseId":"c1","predictedIntent":"JOB_INFO","selectedRoute":"KNOWLEDGE","selectedTool":"NONE","retrievedDocumentIds":["job-1"],"taskSuccess":true,"handoff":false,"latencyMillis":50,"promptTokens":20,"completionTokens":10}
                """);
        OfflineEvaluationRunner runner = new OfflineEvaluationRunner(
                new EvaluationJsonlParser(new ObjectMapper()), new AgentEvaluationService());

        var report = runner.run(cases, results, 5);

        assertThat(report.datasetVersion()).isEqualTo("demo/v1");
        assertThat(report.intent().accuracy()).isEqualTo(1.0);
        assertThat(report.retrieval().recallAtK()).isEqualTo(1.0);
        assertThat(report.endToEnd().successRate()).isEqualTo(1.0);
    }
}
