package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.candidate.domain.Candidate;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ReplySuggestionDraft;
import com.recruit.agent.conversation.domain.ReplySuggestionGenerator;
import com.recruit.agent.job.domain.Job;

import java.util.List;

public class ContextRecoveringReplySuggestionGenerator implements ReplySuggestionGenerator {
    private final ReplySuggestionGenerator primary;
    private final ReplySuggestionGenerator fallback;
    private final ConversationMessageCompactor compactor;
    private final int retryTokenBudget;

    public ContextRecoveringReplySuggestionGenerator(ReplySuggestionGenerator primary,
                                                      ReplySuggestionGenerator fallback,
                                                      ConversationMessageCompactor compactor,
                                                      int retryTokenBudget) {
        this.primary = primary;
        this.fallback = fallback;
        this.compactor = compactor;
        if (retryTokenBudget < 32) throw new IllegalArgumentException("retryTokenBudget must be at least 32");
        this.retryTokenBudget = retryTokenBudget;
    }

    @Override
    public ReplySuggestionDraft generate(Job job, Candidate candidate, Conversation conversation,
                                         List<ConversationMessage> messages) {
        try {
            return primary.generate(job, candidate, conversation, messages);
        } catch (ContextWindowExceededException firstFailure) {
            List<ConversationMessage> compacted = compactor.compact(messages, retryTokenBudget);
            try {
                return primary.generate(job, candidate, conversation, compacted);
            } catch (RuntimeException secondFailure) {
                return fallback.generate(job, candidate, conversation, compacted);
            }
        } catch (RuntimeException failure) {
            return fallback.generate(job, candidate, conversation, messages);
        }
    }
}
