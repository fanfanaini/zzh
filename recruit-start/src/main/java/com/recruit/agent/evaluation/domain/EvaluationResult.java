package com.recruit.agent.evaluation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvaluationResult(
        String schemaVersion,
        String datasetVersion,
        String caseId,
        String predictedIntent,
        String selectedRoute,
        String selectedTool,
        List<String> retrievedDocumentIds,
        boolean taskSuccess,
        boolean handoff,
        Long latencyMillis,
        Integer promptTokens,
        Integer completionTokens,
        String errorComponent,
        String errorCode
) {
    public static final String SCHEMA_VERSION = "agent-evaluation-result/v1";

    public EvaluationResult {
        retrievedDocumentIds = retrievedDocumentIds == null ? List.of() : List.copyOf(retrievedDocumentIds);
    }
}
