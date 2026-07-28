package com.recruit.agent.vector.application;

import com.recruit.agent.vector.infrastructure.DeterministicEmbeddingService;
import com.recruit.agent.vector.infrastructure.InMemoryVectorSearchRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingIndexGovernanceServiceTest {

    @Test
    void shouldRegisterRebuildActivateAndRollbackEmbeddingVersions() {
        InMemoryVectorSearchRepository repository = new InMemoryVectorSearchRepository();
        DeterministicEmbeddingService embeddingService = new DeterministicEmbeddingService();
        EmbeddingIndexGovernanceService service = new EmbeddingIndexGovernanceService(embeddingService, repository);
        repository.saveKnowledgeChunk(1L, 10L, "faq", "Java backend interview process", embeddingService.embed("old"));

        var version = service.register(new RegisterEmbeddingVersionCommand(
                "bge-m3-v1", "BAAI/bge-m3", 1536, "candidate retrieval upgrade"));
        var job = service.rebuildKnowledgeIndex(version.versionId());
        service.activate(version.versionId());
        service.rollback("legacy-default");

        assertThat(version.status()).isEqualTo("READY");
        assertThat(job.status()).isEqualTo("SUCCEEDED");
        assertThat(job.totalChunks()).isEqualTo(1);
        assertThat(service.activeVersion()).isEqualTo("legacy-default");
        assertThat(service.listVersions()).extracting("versionId")
                .containsExactlyInAnyOrder("legacy-default", "bge-m3-v1");
        assertThat(service.listRebuildJobs()).singleElement()
                .satisfies(saved -> assertThat(saved.targetVersion()).isEqualTo("bge-m3-v1"));
    }

    @Test
    void shouldRejectUnknownRebuildTarget() {
        EmbeddingIndexGovernanceService service = new EmbeddingIndexGovernanceService(
                new DeterministicEmbeddingService(), new InMemoryVectorSearchRepository());

        assertThatThrownBy(() -> service.rebuildKnowledgeIndex("missing"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
