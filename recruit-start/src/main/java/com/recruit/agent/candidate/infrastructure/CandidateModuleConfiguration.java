package com.recruit.agent.candidate.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.candidate.application.CandidateApplicationService;
import com.recruit.agent.candidate.application.CandidateProfileExtractionService;
import com.recruit.agent.candidate.application.CandidateResumeTextExtractionService;
import com.recruit.agent.candidate.application.ResumeParsingTaskService;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.candidate.domain.ResumeProfileExtractor;
import com.recruit.agent.candidate.domain.ResumeRepository;
import com.recruit.agent.common.document.DocumentTextExtractor;
import com.recruit.agent.common.storage.FileStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import com.recruit.agent.task.application.AgentTaskApplicationService;

@Configuration
public class CandidateModuleConfiguration {

    @Bean
    @Profile("!mysql")
    public CandidateRepository candidateRepository() {
        return new InMemoryCandidateRepository();
    }

    @Bean
    @Profile("!mysql")
    public ResumeRepository resumeRepository() {
        return new InMemoryResumeRepository();
    }

    @Bean
    public CandidateApplicationService candidateApplicationService(
            CandidateRepository candidateRepository,
            ResumeRepository resumeRepository
    ) {
        return new CandidateApplicationService(candidateRepository, resumeRepository);
    }

    @Bean
    public CandidateResumeTextExtractionService candidateResumeTextExtractionService(
            ResumeRepository resumeRepository,
            FileStorage fileStorage,
            DocumentTextExtractor textExtractor
    ) {
        return new CandidateResumeTextExtractionService(resumeRepository, fileStorage, textExtractor);
    }

    @Bean
    public CandidateProfileExtractionService candidateProfileExtractionService(
            CandidateRepository candidateRepository,
            ResumeRepository resumeRepository,
            ResumeProfileExtractor profileExtractor,
            ObjectMapper objectMapper
    ) {
        return new CandidateProfileExtractionService(candidateRepository, resumeRepository, profileExtractor,
                objectMapper);
    }

    @Bean
    public ResumeProfileExtractor resumeProfileExtractor(LlmService llmService) {
        return new AiResumeProfileExtractor(llmService, new RuleBasedResumeProfileExtractor());
    }

    @Bean
    public ThreadPoolTaskExecutor resumeParsingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("resume-parsing-");
        executor.initialize();
        return executor;
    }

    @Bean
    public ResumeParsingTaskService resumeParsingTaskService(
            AgentTaskApplicationService taskService,
            CandidateResumeTextExtractionService textExtractionService,
            CandidateProfileExtractionService profileExtractionService,
            @Qualifier("resumeParsingTaskExecutor") ThreadPoolTaskExecutor resumeParsingTaskExecutor
    ) {
        return new ResumeParsingTaskService(taskService, textExtractionService, profileExtractionService,
                resumeParsingTaskExecutor);
    }
}
