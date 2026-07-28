package com.recruit.agent.evaluation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.agent.evaluation.domain.RankingMetrics;
import com.recruit.agent.evaluation.domain.RetrievalMetricDelta;
import com.recruit.agent.evaluation.domain.RetrievalQualityReport;
import com.recruit.agent.evaluation.domain.RetrievalRankingSample;
import com.recruit.agent.evaluation.domain.RetrievalWeakCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RetrievalQualityReportService {
    public static final String DATASET_RESOURCE = "evaluation/recruitment-retrieval-quality-v1.jsonl";
    public static final String DATASET_VERSION = "recruitment-retrieval-quality/v1";

    private final ObjectMapper objectMapper;
    private final RetrievalComparisonService comparisonService;
    private final Clock clock;

    public RetrievalQualityReportService(ObjectMapper objectMapper, RetrievalComparisonService comparisonService) {
        this(objectMapper, comparisonService, Clock.systemUTC());
    }

    RetrievalQualityReportService(ObjectMapper objectMapper, RetrievalComparisonService comparisonService,
                                  Clock clock) {
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.comparisonService = java.util.Objects.requireNonNull(comparisonService,
                "comparisonService must not be null");
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
    }

    public RetrievalQualityReport run(InputStream input, int k) throws IOException {
        List<RetrievalRankingSample> samples = parseSamples(input);
        var comparison = comparisonService.compare(samples, k);
        var delta = RetrievalMetricDelta.between(comparison.baseline(), comparison.reranked());
        return new RetrievalQualityReport(DATASET_VERSION, clock.instant(), comparison, delta,
                verdict(comparison.degradedRate(), delta), weakCases(samples, k));
    }

    public List<RetrievalRankingSample> parseSamples(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Retrieval dataset input must not be null");
        }
        List<RetrievalRankingSample> samples = new ArrayList<>();
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
                    RetrievalRankingSample sample = objectMapper.readValue(line, RetrievalRankingSample.class);
                    validateSample(sample);
                    if (!caseIds.add(sample.caseId())) {
                        throw new IllegalArgumentException("duplicate caseId: " + sample.caseId());
                    }
                    samples.add(sample);
                } catch (JsonProcessingException | IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Invalid retrieval JSONL record at line " + lineNumber
                            + ": " + exception.getMessage(), exception);
                }
            }
        }
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("Retrieval JSONL input contains no records");
        }
        return List.copyOf(samples);
    }

    private void validateSample(RetrievalRankingSample sample) {
        if (sample.relevantChunkIds().isEmpty() && sample.relevanceGrades().isEmpty()) {
            throw new IllegalArgumentException("sample must define relevant chunks or relevance grades");
        }
        if (sample.candidateChunkIds().isEmpty()) {
            throw new IllegalArgumentException("candidateChunkIds must not be empty");
        }
        if (sample.baselineChunkIds().isEmpty()) {
            throw new IllegalArgumentException("baselineChunkIds must not be empty");
        }
        if (sample.rerankedChunkIds().isEmpty()) {
            throw new IllegalArgumentException("rerankedChunkIds must not be empty");
        }
    }

    private String verdict(double degradedRate, RetrievalMetricDelta delta) {
        if (degradedRate > 0.2) {
            return "NEEDS_ATTENTION";
        }
        if (delta.ndcgAtK() >= 0.05 && delta.mrrAtK() >= 0) {
            return "IMPROVED";
        }
        if (delta.ndcgAtK() < -0.01 || delta.mrrAtK() < -0.01) {
            return "REGRESSED";
        }
        return "STABLE";
    }

    private List<RetrievalWeakCase> weakCases(List<RetrievalRankingSample> samples, int k) {
        return samples.stream()
                .map(sample -> weakCase(sample, k))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private RetrievalWeakCase weakCase(RetrievalRankingSample sample, int k) {
        RankingMetrics baseline = comparisonService.singleSampleMetrics(sample, k, false);
        RankingMetrics reranked = comparisonService.singleSampleMetrics(sample, k, true);
        if (sample.degraded()) {
            return new RetrievalWeakCase(sample.caseId(), baseline.mrrAtK(), reranked.mrrAtK(),
                    baseline.ndcgAtK(), reranked.ndcgAtK(), "reranker_degraded");
        }
        if (reranked.hitRateAtK() == 0) {
            return new RetrievalWeakCase(sample.caseId(), baseline.mrrAtK(), reranked.mrrAtK(),
                    baseline.ndcgAtK(), reranked.ndcgAtK(), "missed_relevant_chunk");
        }
        if (reranked.ndcgAtK() + 0.01 < baseline.ndcgAtK()) {
            return new RetrievalWeakCase(sample.caseId(), baseline.mrrAtK(), reranked.mrrAtK(),
                    baseline.ndcgAtK(), reranked.ndcgAtK(), "rerank_regression");
        }
        return null;
    }
}
