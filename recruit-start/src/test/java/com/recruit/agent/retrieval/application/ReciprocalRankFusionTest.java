package com.recruit.agent.retrieval.application;

import com.recruit.agent.retrieval.domain.RetrievalHit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReciprocalRankFusionTest {

    private final ReciprocalRankFusion fusion = new ReciprocalRankFusion(60);

    @Test
    void shouldFuseRanksAndDeduplicateChunksAcrossRetrievers() {
        var dense = List.of(hit(1, 101, "dense-a", 91), hit(1, 102, "shared", 82));
        var sparse = List.of(hit(1, 102, "shared", 12), hit(2, 201, "sparse-c", 8));

        var results = fusion.fuse(dense, sparse, 10);

        assertThat(results).extracting(result -> result.chunkId())
                .containsExactly(102L, 101L, 201L);
        assertThat(results.get(0).denseRank()).isEqualTo(2);
        assertThat(results.get(0).sparseRank()).isEqualTo(1);
        assertThat(results.get(0).denseScore()).isEqualTo(82);
        assertThat(results.get(0).sparseScore()).isEqualTo(12);
        assertThat(results.get(0).fusedScore()).isGreaterThan(results.get(1).fusedScore());
    }

    @Test
    void shouldKeepFirstOccurrenceWithinAChannelAndRespectLimit() {
        var dense = List.of(
                hit(1, 101, "first", 90),
                hit(1, 101, "duplicate", 99),
                hit(1, 102, "second", 80)
        );

        var results = fusion.fuse(dense, List.of(), 1);

        assertThat(results).singleElement().satisfies(result -> {
            assertThat(result.chunkId()).isEqualTo(101L);
            assertThat(result.content()).isEqualTo("first");
            assertThat(result.denseRank()).isEqualTo(1);
            assertThat(result.denseScore()).isEqualTo(90);
        });
    }

    @Test
    void shouldUseChunkIdAsDeterministicTieBreaker() {
        var dense = List.of(hit(2, 202, "dense", 90));
        var sparse = List.of(hit(1, 101, "sparse", 10));

        var results = fusion.fuse(dense, sparse, 10);

        assertThat(results).extracting(result -> result.chunkId()).containsExactly(101L, 202L);
    }

    @Test
    void shouldReturnEmptyForEmptyInputsOrZeroLimit() {
        assertThat(fusion.fuse(List.of(), List.of(), 10)).isEmpty();
        assertThat(fusion.fuse(List.of(hit(1, 1, "a", 1)), List.of(), 0)).isEmpty();
    }

    private RetrievalHit hit(long documentId, long chunkId, String content, double score) {
        return new RetrievalHit(documentId, chunkId, "source", content, score);
    }
}
