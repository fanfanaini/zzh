package com.recruit.agent.evaluation.domain;

import java.time.Instant;
import java.util.List;

public record RetrievalQualityReport(
        String datasetVersion,
        Instant generatedAt,
        RetrievalComparisonReport comparison,
        RetrievalMetricDelta delta,
        String verdict,
        List<RetrievalWeakCase> weakCases
) {
    public RetrievalQualityReport {
        if (datasetVersion == null || datasetVersion.isBlank()) {
            throw new IllegalArgumentException("datasetVersion must not be blank");
        }
        generatedAt = generatedAt == null ? Instant.EPOCH : generatedAt;
        weakCases = weakCases == null ? List.of() : List.copyOf(weakCases);
    }
}
