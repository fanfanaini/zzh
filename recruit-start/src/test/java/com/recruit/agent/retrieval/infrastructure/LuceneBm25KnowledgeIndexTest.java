package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.domain.RetrievalFilters;
import com.recruit.agent.retrieval.domain.RetrievalRequest;
import com.recruit.agent.vector.domain.KnowledgeChunk;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LuceneBm25KnowledgeIndexTest {
    private final LuceneBm25KnowledgeIndex index = new LuceneBm25KnowledgeIndex(new Bm25Properties());

    @AfterEach
    void close() {
        index.close();
    }

    @Test
    void retrievesChineseAndEnglishTermsWithBm25() {
        index.rebuild(List.of(
                chunk(1, 11, "Java 岗位", "招聘 Java 后端工程师，要求熟悉 Spring Boot 和 Redis"),
                chunk(2, 21, "招聘制度", "薪资范围需要由 HR 人工确认，不得自动承诺 offer")
        ));

        assertThat(index.search("Spring Boot Java", request(5)).get(0).chunkId()).isEqualTo(11L);
        assertThat(index.search("薪资人工确认", request(5)).get(0).chunkId()).isEqualTo(21L);
    }

    @Test
    void appliesMetadataFiltersAndSupportsIncrementalDeletion() {
        index.rebuild(List.of(
                chunk(1, 11, "policy", "面试安排制度"),
                chunk(2, 21, "faq", "面试安排常见问题")
        ));
        RetrievalRequest filtered = new RetrievalRequest(
                5, 10, 0, new RetrievalFilters(Set.of(2L), Set.of("faq")));

        assertThat(index.search("面试安排", filtered))
                .extracting(result -> result.chunkId())
                .containsExactly(21L);

        index.upsertDocument(List.of(chunk(3, 31, "calendar", "面试日历可用时间")));
        assertThat(index.search("日历可用时间", request(5)))
                .extracting(result -> result.chunkId())
                .contains(31L);

        index.deleteDocument(3L);
        assertThat(index.search("日历可用时间", request(5)))
                .extracting(result -> result.chunkId())
                .doesNotContain(31L);
    }

    private RetrievalRequest request(int limit) {
        return new RetrievalRequest(limit, Math.max(limit, 10), 0, RetrievalFilters.none());
    }

    private KnowledgeChunk chunk(long documentId, long chunkId, String source, String content) {
        return new KnowledgeChunk(documentId, chunkId, source, content);
    }
}
