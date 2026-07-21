package com.recruit.agent.vector.infrastructure;

import com.recruit.agent.vector.application.VectorApplicationService;
import com.recruit.agent.vector.domain.EmbeddingService;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@EnableConfigurationProperties({VectorProperties.class, EmbeddingProperties.class})
public class VectorModuleConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "recruit.embedding", name = "enabled", havingValue = "false", matchIfMissing = true)
    public EmbeddingService embeddingService() {
        return new DeterministicEmbeddingService();
    }

    @Bean
    @ConditionalOnProperty(prefix = "recruit.embedding", name = "enabled", havingValue = "true")
    public EmbeddingService remoteEmbeddingService(EmbeddingProperties properties, RestClient.Builder builder,
                                                   ObjectMapper objectMapper) {
        return new OpenAiCompatibleEmbeddingService(properties, builder, objectMapper);
    }

    @Bean
    public VectorApplicationService vectorApplicationService(
            EmbeddingService embeddingService,
            VectorSearchRepository repository
    ) {
        return new VectorApplicationService(embeddingService, repository);
    }

    @Bean
    @Profile("vector")
    public org.springframework.beans.factory.InitializingBean vectorDatabaseInitializer(VectorProperties properties) {
        return () -> {
            DriverManagerDataSource dataSource = new DriverManagerDataSource(
                    properties.getUrl(), properties.getUsername(), properties.getPassword());
            dataSource.setDriverClassName(properties.getDriverClassName());
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                    new ClassPathResource("db/postgresql/V1__init_vector_tables.sql"),
                    new ClassPathResource("db/postgresql/V2__add_knowledge_embeddings.sql")
            );
            populator.execute(dataSource);
        };
    }
}
