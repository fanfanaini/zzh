package com.recruit.agent.retrieval.application;

import com.recruit.agent.retrieval.domain.DenseRetriever;
import com.recruit.agent.retrieval.domain.HybridRetrievalOutcome;
import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import com.recruit.agent.retrieval.domain.RetrievalChannelDiagnostic;
import com.recruit.agent.retrieval.domain.RetrievalHit;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.retrieval.domain.SparseRetriever;

import java.util.List;

public class HybridRetriever {

    private final DenseRetriever denseRetriever;
    private final SparseRetriever sparseRetriever;
    private final ReciprocalRankFusion fusion;

    public HybridRetriever(DenseRetriever denseRetriever, SparseRetriever sparseRetriever,
                           ReciprocalRankFusion fusion) {
        this.denseRetriever = denseRetriever;
        this.sparseRetriever = sparseRetriever;
        this.fusion = fusion;
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

        ChannelResult dense = retrieveDense(queryEmbedding, request);
        ChannelResult sparse = retrieveSparse(query, request);
        return new HybridRetrievalOutcome(
                fusion.fuse(dense.hits(), sparse.hits(), request.limit()),
                dense.diagnostic(),
                sparse.diagnostic()
        );
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

    private record ChannelResult(List<RetrievalHit> hits, RetrievalChannelDiagnostic diagnostic) {}
}
