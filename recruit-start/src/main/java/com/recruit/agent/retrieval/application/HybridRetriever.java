package com.recruit.agent.retrieval.application;

import com.recruit.agent.retrieval.domain.DenseRetriever;
import com.recruit.agent.retrieval.domain.CandidateReranker;
import com.recruit.agent.retrieval.domain.HybridRetrievalOutcome;
import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.recruit.agent.retrieval.domain.RetrievalChannelDiagnostic;
import com.recruit.agent.retrieval.domain.RetrievalHit;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.retrieval.domain.SparseRetriever;
import com.recruit.agent.retrieval.domain.RerankOutcome;
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
    private final CandidateReranker reranker;

    public HybridRetriever(DenseRetriever denseRetriever, SparseRetriever sparseRetriever,
                           ReciprocalRankFusion fusion) {
        this(denseRetriever, sparseRetriever, fusion, null);
    }

    public HybridRetriever(DenseRetriever denseRetriever, SparseRetriever sparseRetriever,
                           ReciprocalRankFusion fusion, UnifiedTraceRecorder traceRecorder) {
        this(denseRetriever, sparseRetriever, fusion, traceRecorder, null);
    }

    public HybridRetriever(DenseRetriever denseRetriever, SparseRetriever sparseRetriever,
                           ReciprocalRankFusion fusion, UnifiedTraceRecorder traceRecorder,
                           CandidateReranker reranker) {
        this.denseRetriever = denseRetriever;
        this.sparseRetriever = sparseRetriever;
        this.fusion = fusion;
        this.traceRecorder = traceRecorder;
        this.reranker = reranker;
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
        validateRequest(query, request);
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("queryEmbedding must not be empty");
        }
        return searchInternal(query, queryEmbedding, request, true);
    }

    public List<RankedRetrievalResult> searchSparseOnly(String query, RetrievalRequest request) {
        validateRequest(query, request);
        HybridRetrievalOutcome outcome = searchInternal(query, null, request, false);
        if (!outcome.sparse().succeeded()) {
            throw new HybridRetrievalException(outcome);
        }
        return outcome.results();
    }

    private void validateRequest(String query, RetrievalRequest request) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
    }

    private HybridRetrievalOutcome searchInternal(
            String query,
            float[] queryEmbedding,
            RetrievalRequest request,
            boolean denseRequested
    ) {
        UnifiedTraceRecorder.Span span = traceRecorder == null
                ? null
                : traceRecorder.start(TraceComponent.RETRIEVAL, "hybrid_search",
                Map.of("queryLength", query.length(), "limit", request.limit(),
                        "candidateLimit", request.candidateLimit(), "denseRequested", denseRequested));
        try {
            ChannelResult dense = denseRequested
                    ? retrieveDense(queryEmbedding, request)
                    : new ChannelResult(List.of(), RetrievalChannelDiagnostic.failure(
                    new IllegalStateException("Query embedding unavailable")));
            ChannelResult sparse = retrieveSparse(query, request);
            List<RankedRetrievalResult> fused = fusion.fuse(
                    dense.hits(), sparse.hits(), request.candidateLimit());
            RerankOutcome reranked = rerank(query, fused, request.limit());
            HybridRetrievalOutcome outcome = new HybridRetrievalOutcome(
                    reranked.results(),
                    dense.diagnostic(),
                    sparse.diagnostic()
            );
            if (span != null) {
                Map<String, Object> details = diagnostics(outcome, reranked);
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

    private RerankOutcome rerank(String query, List<RankedRetrievalResult> fused, int limit) {
        List<RankedRetrievalResult> fallback = fused.stream().limit(limit).toList();
        if (reranker == null) {
            return new RerankOutcome(fallback, "rrf", false);
        }
        try {
            RerankOutcome outcome = reranker.rerank(query, fused, limit);
            if (!validRerank(outcome, fused, limit)) {
                return new RerankOutcome(fallback, "rrf-invalid-reranker-fallback", true);
            }
            return outcome;
        } catch (RuntimeException exception) {
            return new RerankOutcome(fallback, "rrf-reranker-error-fallback", true);
        }
    }

    private boolean validRerank(RerankOutcome outcome, List<RankedRetrievalResult> candidates, int limit) {
        if (outcome == null || outcome.results().size() > limit) {
            return false;
        }
        java.util.Set<Long> candidateIds = candidates.stream()
                .map(RankedRetrievalResult::chunkId).collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> resultIds = new java.util.HashSet<>();
        return outcome.results().stream()
                .allMatch(result -> result != null
                        && candidateIds.contains(result.chunkId())
                        && resultIds.add(result.chunkId()));
    }

    private Map<String, Object> diagnostics(HybridRetrievalOutcome outcome, RerankOutcome reranked) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("resultCount", outcome.results().size());
        details.put("degraded", outcome.degraded());
        details.put("denseSucceeded", outcome.dense().succeeded());
        details.put("denseHitCount", outcome.dense().hitCount());
        details.put("sparseSucceeded", outcome.sparse().succeeded());
        details.put("sparseHitCount", outcome.sparse().hitCount());
        details.put("rerankerStrategy", reranked.strategy());
        details.put("rerankerDegraded", reranked.degraded());
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
