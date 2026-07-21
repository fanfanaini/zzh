package com.recruit.agent.retrieval.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetrievalRequestTest {

    @Test
    void shouldDefensivelyCopyMetadataFilters() {
        var documents = new HashSet<>(Set.of(1L));
        var filters = new RetrievalFilters(documents, Set.of("policy"));
        documents.add(2L);

        assertThat(filters.documentIds()).containsExactly(1L);
        assertThatThrownBy(() -> filters.documentIds().add(3L))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldValidateLimitsAndDenseScore() {
        assertThatThrownBy(() -> new RetrievalRequest(0, 10, 0, RetrievalFilters.none()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetrievalRequest(10, 9, 0, RetrievalFilters.none()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetrievalRequest(10, 20, 101, RetrievalFilters.none()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
