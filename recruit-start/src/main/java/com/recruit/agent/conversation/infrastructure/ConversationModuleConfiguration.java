package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.ai.domain.LlmService;
import com.recruit.agent.candidate.domain.CandidateRepository;
import com.recruit.agent.conversation.application.ConversationApplicationService;
import com.recruit.agent.conversation.domain.ConversationMessageRepository;
import com.recruit.agent.conversation.domain.ConversationRepository;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.conversation.domain.ReplySuggestionRepository;
import com.recruit.agent.job.domain.JobRepository;
import com.recruit.agent.knowledge.application.KnowledgeApplicationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConversationModuleConfiguration {

    @Bean
    public ReplySuggestionGenerator replySuggestionGenerator(
            LlmService llmService,
            ObjectProvider<KnowledgeApplicationService> knowledgeService
    ) {
        ReplySuggestionGenerator generatedReply = new AiReplySuggestionGenerator(
                llmService,
                new RuleBasedReplySuggestionGenerator(),
                knowledgeService.getIfAvailable()
        );
        return new ClassifyingReplySuggestionGenerator(
                generatedReply,
                new BudgetedConversationMemoryAssembler(),
                new RuleBasedConversationIntentClassifier(),
                new DeterministicConversationRoutingPolicy()
        );
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
