package com.recruit.agent.retrieval.application;

import com.recruit.agent.retrieval.domain.DenseRetriever;
import com.recruit.agent.retrieval.domain.RetrievalFilters;
import com.recruit.agent.retrieval.domain.RetrievalHit;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.retrieval.domain.SparseRetriever;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridRetrieverTest {

    @Test
    void shouldPassMetadataFiltersAndCandidateLimitToBothRetrievers() {
        var filters = new RetrievalFilters(java.util.Set.of(7L), java.util.Set.of("policy"));
        var request = new RetrievalRequest(2, 6, 35, filters);
        var dense = new RecordingDenseRetriever(List.of(hit(7, 1, 80)));
        var sparse = new RecordingSparseRetriever(List.of(hit(7, 2, 10)));
        var retriever = new HybridRetriever(dense, sparse, new ReciprocalRankFusion(60));

        var results = retriever.search("salary policy", new float[]{1, 0}, request);

        assertThat(results).hasSize(2);
        assertThat(dense.request).isEqualTo(request);
        assertThat(sparse.request).isEqualTo(request);
    }

    @Test
    void shouldFallBackToSparseResultsWhenDenseRetrievalFails() {
        DenseRetriever dense = (embedding, request) -> {
            throw new IllegalStateException("vector unavailable");
        };
        SparseRetriever sparse = (query, request) -> List.of(hit(1, 11, 9));
        var retriever = new HybridRetriever(dense, sparse, new ReciprocalRankFusion(60));

        var results = retriever.search("java", new float[]{1}, RetrievalRequest.of(5, 0));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.chunkId()).isEqualTo(11L);
            assertThat(result.denseRank()).isNull();
            assertThat(result.sparseRank()).isEqualTo(1);
        });
    }

    @Test
    void shouldFallBackToDenseResultsWhenSparseRetrievalFails() {
        DenseRetriever dense = (embedding, request) -> List.of(hit(1, 12, 88));
        SparseRetriever sparse = (query, request) -> {
            throw new IllegalStateException("lexical unavailable");
        };
        var retriever = new HybridRetriever(dense, sparse, new ReciprocalRankFusion(60));

        var results = retriever.search("java", new float[]{1}, RetrievalRequest.of(5, 0));

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.chunkId()).isEqualTo(12L);
            assertThat(result.denseRank()).isEqualTo(1);
            assertThat(result.sparseRank()).isNull();
        });
    }

    @Test
    void shouldExposeDiagnosticsAndFailWhenBothRetrieversFail() {
        DenseRetriever dense = (embedding, request) -> { throw new IllegalStateException("dense"); };
        SparseRetriever sparse = (query, request) -> { throw new IllegalStateException("sparse"); };
        var retriever = new HybridRetriever(dense, sparse, new ReciprocalRankFusion(60));

        var outcome = retriever.searchWithDiagnostics("java", new float[]{1}, RetrievalRequest.of(5, 0));
        assertThat(outcome.results()).isEmpty();
        assertThat(outcome.allChannelsFailed()).isTrue();
        assertThat(outcome.dense().errorMessage()).isEqualTo("dense");
        assertThat(outcome.sparse().errorMessage()).isEqualTo("sparse");
        assertThatThrownBy(() -> retriever.search("java", new float[]{1}, RetrievalRequest.of(5, 0)))
                .isInstanceOf(HybridRetrievalException.class);
    }

    @Test
    void shouldRejectBlankQueryAndMissingEmbedding() {
        var retriever = new HybridRetriever((embedding, request) -> List.of(),
                (query, request) -> List.of(), new ReciprocalRankFusion(60));

        assertThatThrownBy(() -> retriever.search(" ", new float[]{1}, RetrievalRequest.of(5, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> retriever.search("java", new float[0], RetrievalRequest.of(5, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private RetrievalHit hit(long documentId, long chunkId, double score) {
        return new RetrievalHit(documentId, chunkId, "source", "content", score);
    }

    private static final class RecordingDenseRetriever implements DenseRetriever {
        private final List<RetrievalHit> results;
        private RetrievalRequest request;

        private RecordingDenseRetriever(List<RetrievalHit> results) {
            this.results = results;
        }

        @Override
        public List<RetrievalHit> search(float[] queryEmbedding, RetrievalRequest request) {
            this.request = request;
            return results;
        }
    }

    private static final class RecordingSparseRetriever implements SparseRetriever {
        private final List<RetrievalHit> results;
        private RetrievalRequest request;

        private RecordingSparseRetriever(List<RetrievalHit> results) {
            this.results = results;
        }

        @Override
        public List<RetrievalHit> search(String query, RetrievalRequest request) {
            this.request = request;
            return results;
        }
    }
}
