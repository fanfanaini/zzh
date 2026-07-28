package com.recruit.agent.retrieval.infrastructure;

import com.recruit.agent.retrieval.application.HybridRetriever;
import com.recruit.agent.retrieval.application.ReciprocalRankFusion;
import com.recruit.agent.retrieval.domain.DenseRetriever;
import com.recruit.agent.retrieval.domain.SparseRetriever;
import com.recruit.agent.retrieval.domain.KnowledgeSparseIndex;
import com.recruit.agent.retrieval.domain.CandidateReranker;
import com.recruit.agent.trace.UnifiedTraceRecorder;
import com.recruit.agent.vector.domain.VectorSearchRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
@Profile("vector")
@EnableConfigurationProperties({Bm25Properties.class, RerankerProperties.class})
public class HybridRetrievalConfiguration {

    @Bean
    @org.springframework.context.annotation.Primary
    public LuceneBm25KnowledgeIndex bm25KnowledgeIndex(Bm25Properties properties) {
        return new LuceneBm25KnowledgeIndex(properties);
    }

    @Bean
    public Bm25IndexRecoveryCoordinator bm25IndexRecoveryCoordinator(
            KnowledgeSparseIndex index,
            VectorSearchRepository repository
    ) {
        return new Bm25IndexRecoveryCoordinator(index, repository);
    }

    @Bean
    @ConditionalOnProperty(prefix = "recruit.retrieval.reranker", name = "mode",
            havingValue = "heuristic", matchIfMissing = true)
    public HeuristicCandidateReranker heuristicCandidateReranker(RerankerProperties properties) {
        return new HeuristicCandidateReranker(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "recruit.retrieval.reranker", name = "mode", havingValue = "http")
    public HttpJinaCandidateReranker httpJinaCandidateReranker(
            RerankerProperties properties,
            ObjectMapper objectMapper
    ) {
        return new HttpJinaCandidateReranker(properties, objectMapper);
    }

    @Bean
    public HybridRetriever hybridRetriever(DenseRetriever denseRetriever, SparseRetriever sparseRetriever,
                                           UnifiedTraceRecorder traceRecorder,
                                           ObjectProvider<CandidateReranker> reranker) {
        return new HybridRetriever(denseRetriever, sparseRetriever, new ReciprocalRankFusion(60), traceRecorder,
                reranker.getIfAvailable());
    }
}
