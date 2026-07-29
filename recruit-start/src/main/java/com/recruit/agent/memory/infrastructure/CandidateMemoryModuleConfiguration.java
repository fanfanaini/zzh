package com.recruit.agent.memory.infrastructure;

import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.memory.application.CandidateMemoryApplicationService;
import com.recruit.agent.memory.application.CandidateMemoryExtractor;
import com.recruit.agent.memory.domain.CandidateMemoryRepository;
import com.recruit.agent.trace.UnifiedTraceRecorder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CandidateMemoryModuleConfiguration {

    @Bean
    @Profile("!mysql")
    public CandidateMemoryRepository candidateMemoryRepository() {
        return new InMemoryCandidateMemoryRepository();
    }

    @Bean
    public CandidateMemoryExtractor candidateMemoryExtractor() {
        return new CandidateMemoryExtractor();
    }

    @Bean
    public CandidateMemoryApplicationService candidateMemoryApplicationService(
            CandidateRepository candidateRepository,
            CandidateMemoryRepository memoryRepository,
            CandidateMemoryExtractor extractor,
            UnifiedTraceRecorder traceRecorder
    ) {
        return new CandidateMemoryApplicationService(candidateRepository, memoryRepository, extractor, traceRecorder);
    }
}
