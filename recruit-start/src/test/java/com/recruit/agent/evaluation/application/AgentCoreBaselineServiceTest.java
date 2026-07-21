package com.recruit.agent.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AgentCoreBaselineServiceTest {

    @Test
    void executesCurrentClassifierAndRouterAgainstVersionedCases() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AgentCoreBaselineService service = new AgentCoreBaselineService(
                new EvaluationJsonlParser(objectMapper), new AgentEvaluationService());

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(AgentCoreBaselineService.DATASET_RESOURCE)) {
            assertThat(input).isNotNull();
            var report = service.run(input);

            assertThat(report.datasetVersion()).isEqualTo("recruitment-agent-core/v2");
            assertThat(report.intent().accuracy()).isEqualTo(1.0);
            assertThat(report.intent().macroF1()).isEqualTo(1.0);
            assertThat(report.routing().exactMatchRate()).isEqualTo(1.0);
            assertThat(report.toolSelection().exactMatchRate()).isEqualTo(1.0);
            assertThat(report.handoff().f1()).isEqualTo(1.0);
            assertThat(report.endToEnd().successRate()).isEqualTo(0.9);
            assertThat(report.endToEnd().expectedOutcomeMatchRate()).isEqualTo(1.0);
            assertThat(report.retrieval().evaluatedCases()).isZero();

            Path reportDirectory = Path.of("target", "agent-evaluation");
            Files.createDirectories(reportDirectory);
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(reportDirectory.resolve("core-baseline-report.json").toFile(), report);
        }
    }
}
