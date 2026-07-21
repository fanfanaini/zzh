package com.recruit.agent.knowledge.application;

import com.recruit.agent.vector.infrastructure.DeterministicEmbeddingService;
import com.recruit.agent.vector.infrastructure.InMemoryVectorSearchRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeApplicationServiceTest {
    @Test
    void shouldIndexChunksAndReturnSources() {
        var service = new KnowledgeApplicationService(new DeterministicEmbeddingService(),
                new InMemoryVectorSearchRepository());
        var indexed = service.index("HR沟通规范", "薪资问题必须转人工确认。面试时间可以先生成草稿。".repeat(80));

        var matches = service.search("薪资需要人工确认", 3);

        assertThat(indexed.chunkCount()).isGreaterThan(1);
        assertThat(matches).isNotEmpty();
        assertThat(matches.get(0).sourceName()).isEqualTo("HR沟通规范");
        assertThat(service.listDocuments()).singleElement().satisfies(document -> {
            assertThat(document.documentId()).isEqualTo(indexed.documentId());
            assertThat(document.chunkCount()).isEqualTo(indexed.chunkCount());
        });

        service.delete(indexed.documentId());

        assertThat(service.listDocuments()).isEmpty();
        assertThat(service.search("薪资需要人工确认", 3)).isEmpty();
    }

    @Test
    void filtersLowScoreMatchesAndSplitsOnSentenceBoundary() {
        var repository = new InMemoryVectorSearchRepository();
        var service = new KnowledgeApplicationService(
                new DeterministicEmbeddingService(), repository, 200, 20, 101);
        var indexed = service.index("制度", "第一段招聘制度。".repeat(80));

        assertThat(indexed.chunkCount()).isGreaterThan(1);
        assertThat(service.search("完全不同的查询", 5)).isEmpty();
    }
}
