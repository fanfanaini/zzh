package com.recruit.agent.evaluation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvaluationCase(
        String schemaVersion,
        String datasetVersion,
        String caseId,
        String provenance,
        String message,
        String expectedIntent,
        String expectedRoute,
        String expectedTool,
        List<String> relevantDocumentIds,
        boolean expectedTaskSuccess,
        boolean expectedHandoff,
        List<String> tags
) {
    public static final String SCHEMA_VERSION = "agent-evaluation-case/v1";
    public static final String NO_TOOL = "NONE";

    public EvaluationCase {
        relevantDocumentIds = relevantDocumentIds == null ? List.of() : List.copyOf(relevantDocumentIds);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
