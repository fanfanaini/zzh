package com.recruit.agent.job.infrastructure;

import com.recruit.agent.job.application.JobApplicationService;
import com.recruit.agent.job.application.JobAnalysisService;
import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.job.domain.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class JobModuleConfiguration {

    @Bean
    @Profile("!mysql")
    public JobRepository jobRepository() {
        return new InMemoryJobRepository();
    }

    @Bean
    public JobApplicationService jobApplicationService(JobRepository repository) {
        return new JobApplicationService(repository);
    }

    @Bean
    public JobAnalysisService jobAnalysisService(JobApplicationService jobs, LlmService llmService) {
        return new JobAnalysisService(jobs, llmService);
    }
}
