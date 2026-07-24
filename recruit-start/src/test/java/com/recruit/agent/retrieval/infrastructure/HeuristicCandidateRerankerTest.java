package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.domain.RankedRetrievalResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HeuristicCandidateRerankerTest {
    private final RerankerProperties properties = contentOnlyProperties();
    private final HeuristicCandidateReranker reranker = new HeuristicCandidateReranker(properties);

    @AfterEach
    void close() {
        reranker.close();
    }

    @Test
    void promotesCandidatesWithBetterQueryTermCoverage() {
        RankedRetrievalResult highRrfButIrrelevant = result(1, "公司福利制度", 0.04);
        RankedRetrievalResult lowerRrfButRelevant = result(
                2, "Java Spring Boot 后端工程师岗位职责", 0.02);

        var outcome = reranker.rerank(
                "Java Spring Boot 岗位",
                List.of(highRrfButIrrelevant, lowerRrfButRelevant),
                2);

        assertThat(outcome.results()).extracting(RankedRetrievalResult::chunkId)
                .containsExactly(2L, 1L);
        assertThat(outcome.strategy()).isEqualTo("heuristic-smartcn-v1");
    }

    @Test
    void preservesOriginalOrderForEqualScoresAndHonorsLimit() {
        var outcome = reranker.rerank("不存在的词", List.of(
                result(1, "A", 0.04),
                result(2, "B", 0.03),
                result(3, "C", 0.02)
        ), 2);

        assertThat(outcome.results()).extracting(RankedRetrievalResult::chunkId)
                .containsExactly(1L, 2L);
    }

    private RerankerProperties contentOnlyProperties() {
        RerankerProperties value = new RerankerProperties();
        value.setRrfWeight(0);
        value.setDenseWeight(0);
        value.setSparseWeight(0);
        value.setContentCoverageWeight(1);
        value.setSourceCoverageWeight(0);
        return value;
    }

    private RankedRetrievalResult result(long chunkId, String content, double fusedScore) {
        return new RankedRetrievalResult(1L, chunkId, "source", content,
                1, 80.0, 1, 5.0, fusedScore);
    }
}
