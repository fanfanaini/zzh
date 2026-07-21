package com.recruit.agent.knowledge.infrastructure;

import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(KnowledgeProperties.class)
public class KnowledgeModuleConfiguration {
    @Bean
    public KnowledgeApplicationService knowledgeApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository,
            KnowledgeProperties properties
    ) {
        return new KnowledgeApplicationService(
                embeddingService,
                repository,
                properties.getChunkSize(),
                properties.getOverlap(),
                properties.getMinScore()
        );
    }
}
