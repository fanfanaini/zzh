package com.recruit.agent.retrieval.application;

import com.recruit.agent.retrieval.domain.DenseRetriever;
import com.recruit.agent.retrieval.domain.HybridRetrievalOutcome;
import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.recruit.agent.retrieval.domain.RetrievalChannelDiagnostic;
import com.recruit.agent.retrieval.domain.RetrievalHit;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.retrieval.domain.SparseRetriever;
import com.recruit.agent.trace.TraceComponent;
import com.recruit.agent.trace.UnifiedTraceRecorder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HybridRetriever {

    private final DenseRetriever denseRetriever;
    private final SparseRetriever sparseRetriever;
    private final ReciprocalRankFusion fusion;
    private final UnifiedTraceRecorder traceRecorder;

    public HybridRetriever(DenseRetriever denseRetriever, SparseRetriever sparseRetriever,
                           ReciprocalRankFusion fusion) {
        this(denseRetriever, sparseRetriever, fusion, null);
    }

    public HybridRetriever(DenseRetriever denseRetriever, SparseRetriever sparseRetriever,
                           ReciprocalRankFusion fusion, UnifiedTraceRecorder traceRecorder) {
        this.denseRetriever = denseRetriever;
        this.sparseRetriever = sparseRetriever;
        this.fusion = fusion;
        this.traceRecorder = traceRecorder;
    }

    public List<RankedRetrievalResult> search(String query, float[] queryEmbedding, RetrievalRequest request) {
        HybridRetrievalOutcome outcome = searchWithDiagnostics(query, queryEmbedding, request);
        if (outcome.allChannelsFailed()) {
            throw new HybridRetrievalException(outcome);
        }
        return outcome.results();
    }

    public HybridRetrievalOutcome searchWithDiagnostics(
            String query,
            float[] queryEmbedding,
            RetrievalRequest request
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("queryEmbedding must not be empty");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        UnifiedTraceRecorder.Span span = traceRecorder == null
                ? null
                : traceRecorder.start(TraceComponent.RETRIEVAL, "hybrid_search",
                Map.of("queryLength", query.length(), "limit", request.limit(),
                        "candidateLimit", request.candidateLimit()));
        try {
            ChannelResult dense = retrieveDense(queryEmbedding, request);
            ChannelResult sparse = retrieveSparse(query, request);
            HybridRetrievalOutcome outcome = new HybridRetrievalOutcome(
                    fusion.fuse(dense.hits(), sparse.hits(), request.limit()),
                    dense.diagnostic(),
                    sparse.diagnostic()
            );
            if (span != null) {
                Map<String, Object> details = diagnostics(outcome);
                if (outcome.allChannelsFailed()) {
                    span.fail("ALL_CHANNELS_FAILED", "Dense and sparse retrieval both failed", details);
                } else {
                    span.succeed(details);
                }
            }
            return outcome;
        } catch (RuntimeException exception) {
            if (span != null) {
                span.fail(exception.getClass().getSimpleName(), safeMessage(exception), Map.of());
            }
            throw exception;
        }
    }

    private ChannelResult retrieveDense(float[] queryEmbedding, RetrievalRequest request) {
        try {
            List<RetrievalHit> results = denseRetriever.search(queryEmbedding, request);
            List<RetrievalHit> safeResults = results == null ? List.of() : List.copyOf(results);
            return new ChannelResult(safeResults, RetrievalChannelDiagnostic.success(safeResults.size()));
        } catch (RuntimeException exception) {
            return new ChannelResult(List.of(), RetrievalChannelDiagnostic.failure(exception));
        }
    }

    private ChannelResult retrieveSparse(String query, RetrievalRequest request) {
        try {
            List<RetrievalHit> results = sparseRetriever.search(query, request);
            List<RetrievalHit> safeResults = results == null ? List.of() : List.copyOf(results);
            return new ChannelResult(safeResults, RetrievalChannelDiagnostic.success(safeResults.size()));
        } catch (RuntimeException exception) {
            return new ChannelResult(List.of(), RetrievalChannelDiagnostic.failure(exception));
        }
    }

    private Map<String, Object> diagnostics(HybridRetrievalOutcome outcome) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("resultCount", outcome.results().size());
        details.put("degraded", outcome.degraded());
        details.put("denseSucceeded", outcome.dense().succeeded());
        details.put("denseHitCount", outcome.dense().hitCount());
        details.put("sparseSucceeded", outcome.sparse().succeeded());
        details.put("sparseHitCount", outcome.sparse().hitCount());
        if (!outcome.dense().succeeded()) {
            details.put("denseErrorCode", outcome.dense().errorType());
        }
        if (!outcome.sparse().succeeded()) {
            details.put("sparseErrorCode", outcome.sparse().errorType());
        }
        return details;
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private record ChannelResult(List<RetrievalHit> hits, RetrievalChannelDiagnostic diagnostic) {}
}
