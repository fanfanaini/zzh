package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.ai.infrastructure.AiProperties;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.conversation.application.ConversationApplicationService;
import com.recruit.agent.conversation.domain.ConversationMessageRepository;
import com.recruit.agent.conversation.domain.ConversationRepository;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.conversation.domain.ReplySuggestionRepository;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import com.recruit.agent.tool.ToolRuntime;
import com.recruit.agent.trace.AgentTraceSink;
import com.recruit.agent.workflow.LangGraphReplySuggestionGenerator;
import com.recruit.agent.workflow.execution.AgentExecutionContextFactory;
import com.recruit.agent.workflow.execution.SpringSecurityAgentExecutionContextFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConversationModuleConfiguration {

    @Bean
    public ReplySuggestionGenerator replySuggestionGenerator(
            LlmService llmService,
            ObjectProvider<KnowledgeApplicationService> knowledgeService,
            ToolRuntime toolRuntime,
            AgentTraceSink agentTraceSink,
            AgentExecutionContextFactory contextFactory
    ) {
        ReplySuggestionGenerator generatedReply = new AiReplySuggestionGenerator(
                llmService,
                new RuleBasedReplySuggestionGenerator(),
                knowledgeService.getIfAvailable()
        );
        return new LangGraphReplySuggestionGenerator(
                generatedReply,
                new BudgetedConversationMemoryAssembler(),
                new RuleBasedConversationIntentClassifier(),
                new DeterministicConversationRoutingPolicy(),
                toolRuntime,
                agentTraceSink,
                contextFactory
        );
    }

    @Bean
    public AgentExecutionContextFactory agentExecutionContextFactory(AiProperties aiProperties) {
        return new SpringSecurityAgentExecutionContextFactory(aiProperties);
    }

    @Bean
    public ConversationApplicationService conversationApplicationService(
            JobRepository jobRepository,
            CandidateRepository candidateRepository,
            ConversationRepository conversationRepository,
            ConversationMessageRepository messageRepository,
            ReplySuggestionRepository suggestionRepository,
            ReplySuggestionGenerator generator
    ) {
        return new ConversationApplicationService(
                jobRepository,
                candidateRepository,
                conversationRepository,
                messageRepository,
                suggestionRepository,
                generator
        );
    }
}
