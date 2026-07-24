package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.domain.KnowledgeSparseIndex;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

public final class Bm25IndexRecoveryCoordinator {
    private static final Logger log = LoggerFactory.getLogger(Bm25IndexRecoveryCoordinator.class);

    private final KnowledgeSparseIndex index;
    private final VectorSearchRepository repository;

    public Bm25IndexRecoveryCoordinator(KnowledgeSparseIndex index, VectorSearchRepository repository) {
        this.index = index;
        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void rebuild() {
        try {
            index.rebuild(repository.listKnowledgeChunks());
        } catch (RuntimeException exception) {
            log.error("BM25 index rebuild failed; retrieval will degrade to the dense channel", exception);
        }
    }
}
