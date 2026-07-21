package com.recruit.agent.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluationJsonlParserTest {

    private final EvaluationJsonlParser parser = new EvaluationJsonlParser(new ObjectMapper());

    @Test
    void parsesVersionedGoldenDataset() throws Exception {
        Path dataset = findProjectFile("deploy", "seed", "evaluation", "recruitment-golden-v1.jsonl");

        var cases = parser.parseCases(dataset);

        assertThat(cases).hasSize(10);
        assertThat(cases).extracting(value -> value.caseId()).doesNotHaveDuplicates();
        assertThat(cases).allSatisfy(value -> {
            assertThat(value.datasetVersion()).isEqualTo("recruitment-golden/v1");
            assertThat(value.provenance()).isNotBlank();
        });
    }

    @Test
    void reportsLineNumberForUnsupportedRecord() {
        String jsonl = """
                {"schemaVersion":"agent-evaluation-case/v1","datasetVersion":"v1","caseId":"ok","provenance":"curated","message":"hi","expectedIntent":"A","expectedRoute":"B","expectedTool":"NONE","expectedTaskSuccess":true,"expectedHandoff":false}
                {"schemaVersion":"agent-evaluation-case/v2","datasetVersion":"v1","caseId":"bad","provenance":"curated","message":"hi","expectedIntent":"A","expectedRoute":"B","expectedTool":"NONE","expectedTaskSuccess":true,"expectedHandoff":false}
                """;

        assertThatThrownBy(() -> parser.parseCases(bytes(jsonl)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line 2")
                .hasMessageContaining("unsupported schemaVersion");
    }

    @Test
    void rejectsDuplicateCaseIdsAndNegativeRuntimeValues() {
        String duplicateCases = """
                {"schemaVersion":"agent-evaluation-case/v1","datasetVersion":"v1","caseId":"same","provenance":"curated","message":"a","expectedIntent":"A","expectedRoute":"B","expectedTool":"NONE","expectedTaskSuccess":true,"expectedHandoff":false}
                {"schemaVersion":"agent-evaluation-case/v1","datasetVersion":"v1","caseId":"same","provenance":"curated","message":"b","expectedIntent":"A","expectedRoute":"B","expectedTool":"NONE","expectedTaskSuccess":true,"expectedHandoff":false}
                """;
        String invalidResult = """
                {"schemaVersion":"agent-evaluation-result/v1","datasetVersion":"v1","caseId":"same","predictedIntent":"A","selectedRoute":"B","selectedTool":"NONE","taskSuccess":true,"handoff":false,"latencyMillis":-1}
                """;

        assertThatThrownBy(() -> parser.parseCases(bytes(duplicateCases))).hasMessageContaining("duplicate caseId");
        assertThatThrownBy(() -> parser.parseResults(bytes(invalidResult)))
                .hasMessageContaining("latencyMillis must not be negative");
    }

    private ByteArrayInputStream bytes(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private Path findProjectFile(String... segments) {
        Path current = Path.of("").toAbsolutePath();
        for (int depth = 0; depth < 4 && current != null; depth++, current = current.getParent()) {
            Path candidate = current;
            for (String segment : segments) {
                candidate = candidate.resolve(segment);
            }
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("project file not found");
    }
}
