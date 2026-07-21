package com.recruit.agent.evaluation.domain;

import java.util.Map;

public record EvaluationReport(
        String datasetVersion,
        ClassificationMetrics intent,
        ExactMatchMetrics routing,
        ExactMatchMetrics toolSelection,
        RetrievalMetrics retrieval,
        EndToEndMetrics endToEnd,
        BinaryClassificationMetrics handoff,
        RuntimeMetrics runtime,
        Map<String, Long> failureCounts
) {
    public EvaluationReport {
        failureCounts = Map.copyOf(failureCounts);
    }
}
