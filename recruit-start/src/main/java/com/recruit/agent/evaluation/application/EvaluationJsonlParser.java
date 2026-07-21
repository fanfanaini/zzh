package com.recruit.agent.evaluation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.evaluation.domain.EvaluationCase;
import com.recruit.agent.evaluation.domain.EvaluationResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EvaluationJsonlParser {

    private final ObjectMapper objectMapper;

    public EvaluationJsonlParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<EvaluationCase> parseCases(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return parseCases(input);
        }
    }

    public List<EvaluationCase> parseCases(InputStream input) throws IOException {
        return parse(input, EvaluationCase.class, EvaluationCase.SCHEMA_VERSION, this::validateCase);
    }

    public List<EvaluationResult> parseResults(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return parseResults(input);
        }
    }

    public List<EvaluationResult> parseResults(InputStream input) throws IOException {
        return parse(input, EvaluationResult.class, EvaluationResult.SCHEMA_VERSION, this::validateResult);
    }

    private <T> List<T> parse(InputStream input, Class<T> type, String schemaVersion, Validator<T> validator)
            throws IOException {
        List<T> records = new ArrayList<>();
        Set<String> caseIds = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    T record = objectMapper.readValue(line, type);
                    String caseId = validator.validate(record, schemaVersion);
                    if (!caseIds.add(caseId)) {
                        throw new IllegalArgumentException("duplicate caseId: " + caseId);
                    }
                    records.add(record);
                } catch (JsonProcessingException | IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Invalid JSONL record at line " + lineNumber + ": "
                            + exception.getMessage(), exception);
                }
            }
        }
        if (records.isEmpty()) {
            throw new IllegalArgumentException("JSONL input contains no records");
        }
        return List.copyOf(records);
    }

    private String validateCase(EvaluationCase value, String schemaVersion) {
        requireVersion(value.schemaVersion(), schemaVersion);
        requireText(value.datasetVersion(), "datasetVersion");
        requireText(value.caseId(), "caseId");
        requireText(value.provenance(), "provenance");
        requireText(value.message(), "message");
        requireText(value.expectedIntent(), "expectedIntent");
        requireText(value.expectedRoute(), "expectedRoute");
        requireText(value.expectedTool(), "expectedTool");
        return value.caseId();
    }

    private String validateResult(EvaluationResult value, String schemaVersion) {
        requireVersion(value.schemaVersion(), schemaVersion);
        requireText(value.datasetVersion(), "datasetVersion");
        requireText(value.caseId(), "caseId");
        requireText(value.predictedIntent(), "predictedIntent");
        requireText(value.selectedRoute(), "selectedRoute");
        requireText(value.selectedTool(), "selectedTool");
        requireNonNegative(value.latencyMillis(), "latencyMillis");
        requireNonNegative(value.promptTokens(), "promptTokens");
        requireNonNegative(value.completionTokens(), "completionTokens");
        return value.caseId();
    }

    private void requireVersion(String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("unsupported schemaVersion: " + actual);
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private void requireNonNegative(Number value, String field) {
        if (value != null && value.longValue() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }

    @FunctionalInterface
    private interface Validator<T> {
        String validate(T value, String schemaVersion);
    }
}
