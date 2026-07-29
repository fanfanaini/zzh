package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.infrastructure.AiProperties;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.conversation.application.ConversationApplicationService;
import com.recruit.agent.conversation.domain.ConversationMessageRepository;
import com.recruit.agent.conversation.domain.ConversationRepository;
import com.recruit.agent.conversation.domain.ConversationMemoryAssembler;
import com.recruit.agent.conversation.domain.ConversationSummaryProvider;
import com.recruit.agent.conversation.domain.ConversationSummaryRepository;
import com.recruit.agent.conversation.domain.TokenEstimator;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.conversation.domain.ReplySuggestionRepository;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.jobapplication.application.JobApplicationPipelineService;
import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import com.recruit.agent.memory.application.CandidateMemoryApplicationService;
import com.recruit.agent.skill.application.SkillContextAssembler;
import com.recruit.agent.tool.ToolRuntime;
import com.recruit.agent.toolcalling.FunctionCallingToolPlanner;
import com.recruit.agent.trace.AgentTraceSink;
import com.recruit.agent.workflow.LangGraphReplySuggestionGenerator;
import com.recruit.agent.workflow.execution.AgentExecutionContextFactory;
import com.recruit.agent.workflow.execution.SpringSecurityAgentExecutionContextFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ConversationMemoryProperties.class)
public class ConversationModuleConfiguration {

    @Bean
    public ReplySuggestionGenerator replySuggestionGenerator(
            LlmService llmService,
            ObjectProvider<KnowledgeApplicationService> knowledgeService,
            ToolRuntime toolRuntime,
            AgentTraceSink agentTraceSink,
            AgentExecutionContextFactory contextFactory,
            ConversationMemoryAssembler memoryAssembler,
            TokenEstimator tokenEstimator,
            ObjectProvider<SkillContextAssembler> skillContextAssembler,
            FunctionCallingToolPlanner toolPlanner,
            ConversationMemoryProperties memoryProperties
    ) {
        ReplySuggestionGenerator aiReply = new AiReplySuggestionGenerator(
                llmService,
                new RuleBasedReplySuggestionGenerator(),
                knowledgeService.getIfAvailable(),
                skillContextAssembler.getIfAvailable()
        );
        ReplySuggestionGenerator generatedReply = new ContextRecoveringReplySuggestionGenerator(
                aiReply, new RuleBasedReplySuggestionGenerator(),
                new ConversationMessageCompactor(tokenEstimator), memoryProperties.getRetryTokenBudget());
        return new LangGraphReplySuggestionGenerator(
                generatedReply,
                memoryAssembler,
                new RuleBasedConversationIntentClassifier(),
                new DeterministicConversationRoutingPolicy(),
                toolRuntime,
                toolPlanner,
                agentTraceSink,
                contextFactory
        );
    }

    @Bean
    public AgentExecutionContextFactory agentExecutionContextFactory(AiProperties aiProperties) {
        return new SpringSecurityAgentExecutionContextFactory(aiProperties);
    }

    @Bean
    public TokenEstimator conversationTokenEstimator() {
        return new HeuristicTokenEstimator();
    }

    @Bean
    public ConversationSummaryProvider conversationSummaryProvider(ConversationSummaryRepository repository,
                                                                   TokenEstimator tokenEstimator) {
        return new PersistentConversationSummaryProvider(repository, tokenEstimator);
    }

    @Bean
    public ConversationMemoryAssembler conversationMemoryAssembler(ConversationSummaryProvider summaryProvider,
                                                                   TokenEstimator tokenEstimator,
                                                                   ConversationMemoryProperties properties) {
        return new BudgetedConversationMemoryAssembler(properties.getTokenBudget(),
                properties.getRecentMessageLimit(), summaryProvider, tokenEstimator);
    }

    @Bean
    public ConversationApplicationService conversationApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ReplySuggestionRepository suggestionRepository,
            ReplySuggestionGenerator generator,
            CandidateMemoryApplicationService memoryService,
            ObjectProvider<JobApplicationPipelineService> jobApplicationPipelineService
    ) {
        return new ConversationApplicationService(
                jobRepository,
                candidateRepository,
                conversationRepository,
                messageRepository,
                suggestionRepository,
                generator,
                memoryService,
                java.util.Optional.ofNullable(jobApplicationPipelineService.getIfAvailable())
        );
    }
}
