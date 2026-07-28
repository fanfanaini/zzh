package com.recruit.agent.evaluation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetrievalQualityReportServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RetrievalQualityReportService service = new RetrievalQualityReportService(
            objectMapper,
            new RetrievalComparisonService(),
            Clock.fixed(Instant.parse("2026-07-28T00:00:00Z"), ZoneOffset.UTC)
    );

    @Test
    void shouldGenerateReportFromCuratedDataset() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream(
                RetrievalQualityReportService.DATASET_RESOURCE)) {
            var report = service.run(input, 5);

            assertThat(report.datasetVersion()).isEqualTo(RetrievalQualityReportService.DATASET_VERSION);
            assertThat(report.generatedAt()).isEqualTo(Instant.parse("2026-07-28T00:00:00Z"));
            assertThat(report.comparison().cases()).isGreaterThanOrEqualTo(10);
            assertThat(report.delta().mrrAtK()).isGreaterThan(0);
            assertThat(report.delta().ndcgAtK()).isGreaterThan(0);
            assertThat(report.verdict()).isEqualTo("IMPROVED");
            assertThat(report.weakCases()).extracting("caseId")
                    .contains("internship-campus-recruitment-weak-001", "reranker-timeout-fallback-001");
        }
    }

    @Test
    void shouldRejectDuplicateCaseIds() {
        String jsonl = """
                {"caseId":"dup","relevantChunkIds":[1],"candidateChunkIds":[1],"baselineChunkIds":[1],"rerankedChunkIds":[1],"rerankLatencyMillis":1,"degraded":false}
                {"caseId":"dup","relevantChunkIds":[1],"candidateChunkIds":[1],"baselineChunkIds":[1],"rerankedChunkIds":[1],"rerankLatencyMillis":1,"degraded":false}
                """;

        assertThatThrownBy(() -> service.parseSamples(input(jsonl)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate caseId");
    }

    @Test
    void shouldRejectSamplesWithoutRelevantChunks() {
        String jsonl = """
                {"caseId":"bad","candidateChunkIds":[1],"baselineChunkIds":[1],"rerankedChunkIds":[1],"rerankLatencyMillis":1,"degraded":false}
                """;

        assertThatThrownBy(() -> service.parseSamples(input(jsonl)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relevant chunks");
    }

    private ByteArrayInputStream input(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
