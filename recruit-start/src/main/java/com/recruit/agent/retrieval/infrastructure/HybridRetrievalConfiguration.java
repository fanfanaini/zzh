package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.application.HybridRetriever;
import com.recruit.agent.retrieval.application.ReciprocalRankFusion;
import com.recruit.agent.retrieval.domain.DenseRetriever;
import com.recruit.agent.retrieval.domain.SparseRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("vector")
public class HybridRetrievalConfiguration {

    @Bean
    public HybridRetriever hybridRetriever(DenseRetriever denseRetriever, SparseRetriever sparseRetriever) {
        return new HybridRetriever(denseRetriever, sparseRetriever, new ReciprocalRankFusion(60));
    }
}
