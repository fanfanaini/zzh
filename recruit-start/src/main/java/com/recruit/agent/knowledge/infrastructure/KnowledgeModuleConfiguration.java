package com.recruit.agent.knowledge.infrastructure;

import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import com.recruit.agent.knowledge.application.KnowledgeQueryRewriter;
import com.recruit.agent.retrieval.application.HybridRetriever;
import com.recruit.agent.retrieval.domain.KnowledgeSparseIndex;
import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(KnowledgeProperties.class)
public class KnowledgeModuleConfiguration {
    @Bean
    public KnowledgeQueryRewriter knowledgeQueryRewriter() {
        return new RecruitmentKnowledgeQueryRewriter();
    }

    @Bean
    public KnowledgeApplicationService knowledgeApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository,
            KnowledgeProperties properties,
            ObjectProvider<HybridRetriever> hybridRetriever,
            ObjectProvider<KnowledgeSparseIndex> sparseIndex,
            KnowledgeQueryRewriter queryRewriter
    ) {
        return new KnowledgeApplicationService(
                embeddingService,
                repository,
                properties.getChunkSize(),
                properties.getOverlap(),
                properties.getMinScore(),
                hybridRetriever.getIfAvailable(),
                sparseIndex.getIfAvailable(),
                queryRewriter,
                properties.isMultiQueryEnabled(),
                properties.getMultiQueryMaxQueries(),
                properties.getMultiQueryCandidateMultiplier()
        );
    }
}
