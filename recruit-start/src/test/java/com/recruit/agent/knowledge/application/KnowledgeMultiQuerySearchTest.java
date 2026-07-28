package com.recruit.agent.knowledge.application;

import com.recruit.agent.retrieval.application.HybridRetriever;
import com.recruit.agent.retrieval.application.ReciprocalRankFusion;
import com.recruit.agent.retrieval.domain.RetrievalHit;
import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.infrastructure.InMemoryVectorSearchRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeMultiQuerySearchTest {

    @Test
    void shouldExpandRecruitmentQueryAndMergeResultsAcrossRewrites() {
        EmbeddingService embedding = text -> new float[]{1};
        List<String> sparseQueries = new CopyOnWriteArrayList<>();
        HybridRetriever hybridRetriever = new HybridRetriever(
                (queryEmbedding, request) -> List.of(),
                (query, request) -> {
                    sparseQueries.add(query);
                    if (query.contains("薪酬承诺")) {
                        return List.of(new RetrievalHit(7L, 70L, "salary-policy",
                                "薪资范围只能说明区间，不能承诺最终 offer 金额。", 12.0));
                    }
                    return List.of();
                },
                new ReciprocalRankFusion(60));
        KnowledgeApplicationService service = new KnowledgeApplicationService(
                embedding,
                new InMemoryVectorSearchRepository(),
                200,
                20,
                0,
                hybridRetriever,
                null,
                (query, maxQueries) -> List.of(query, query + " 薪酬承诺 offer 条款 人工确认"),
                true,
                3,
                3);

        var results = service.search("薪资可以保证吗", 3);

        assertThat(sparseQueries).hasSize(2);
        assertThat(results).singleElement().satisfies(match -> {
            assertThat(match.chunkId()).isEqualTo(70L);
            assertThat(match.sourceName()).isEqualTo("salary-policy");
        });
    }

    @Test
    void shouldKeepSingleQueryBehaviorWhenMultiQueryDisabled() {
        EmbeddingService embedding = text -> new float[]{1};
        List<String> sparseQueries = new CopyOnWriteArrayList<>();
        HybridRetriever hybridRetriever = new HybridRetriever(
                (queryEmbedding, request) -> List.of(),
                (query, request) -> {
                    sparseQueries.add(query);
                    return List.of(new RetrievalHit(1L, 10L, "policy", "content", 8.0));
                },
                new ReciprocalRankFusion(60));
        KnowledgeApplicationService service = new KnowledgeApplicationService(
                embedding,
                new InMemoryVectorSearchRepository(),
                200,
                20,
                0,
                hybridRetriever,
                null,
                (query, maxQueries) -> List.of(query, query + " expansion"),
                false,
                3,
                3);

        service.search("面试安排", 3);

        assertThat(sparseQueries).containsExactly("面试安排");
    }
}
