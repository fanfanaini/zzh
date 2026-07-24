package com.recruit.agent.knowledge.application;

import com.recruit.agent.vector.infrastructure.DeterministicEmbeddingService;
import com.recruit.agent.vector.infrastructure.InMemoryVectorSearchRepository;
import org.junit.jupiter.api.Test;
import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import com.recruit.agent.retrieval.application.HybridRetriever;
import com.recruit.agent.retrieval.application.ReciprocalRankFusion;
import com.recruit.agent.retrieval.domain.RetrievalHit;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void doesNotPersistAPartialDocumentWhenEmbeddingFails() {
        EmbeddingService embedding = mock(EmbeddingService.class);
        VectorSearchRepository repository = mock(VectorSearchRepository.class);
        when(embedding.embed(anyString()))
                .thenReturn(new float[]{1})
                .thenThrow(new IllegalStateException("embedding unavailable"));
        var service = new KnowledgeApplicationService(embedding, repository, 200, 20, 0);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        service.index("policy", "Java 招聘制度。".repeat(80)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("embedding unavailable");

        verify(repository, never()).saveKnowledgeDocument(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void persistsAllChunksAsOneDocumentOperation() {
        EmbeddingService embedding = text -> new float[]{1};
        VectorSearchRepository repository = mock(VectorSearchRepository.class);
        var service = new KnowledgeApplicationService(embedding, repository, 200, 20, 0);

        var indexed = service.index("policy", "Java 招聘制度。".repeat(80));

        verify(repository).saveKnowledgeDocument(org.mockito.ArgumentMatchers.argThat(
                chunks -> chunks.size() == indexed.chunkCount()
                        && chunks.stream().allMatch(chunk -> chunk.documentId().equals(indexed.documentId()))));
    }

    @Test
    void fallsBackToSparseRetrievalWhenQueryEmbeddingIsUnavailable() {
        EmbeddingService embedding = text -> {
            throw new IllegalStateException("embedding provider down");
        };
        VectorSearchRepository repository = mock(VectorSearchRepository.class);
        HybridRetriever hybrid = new HybridRetriever(
                (vector, request) -> {
                    throw new AssertionError("dense retriever must not run without an embedding");
                },
                (query, request) -> java.util.List.of(
                        new RetrievalHit(1L, 2L, "policy", "薪资需要人工确认", 3.5)),
                new ReciprocalRankFusion(60));
        var service = new KnowledgeApplicationService(
                embedding, repository, 200, 20, 0, hybrid);

        assertThat(service.search("薪资确认", 3)).singleElement().satisfies(match -> {
            assertThat(match.chunkId()).isEqualTo(2L);
            assertThat(match.sourceName()).isEqualTo("policy");
        });
    }
}
