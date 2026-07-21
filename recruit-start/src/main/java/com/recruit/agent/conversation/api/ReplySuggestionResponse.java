package com.recruit.agent.conversation.api;

import com.recruit.agent.conversation.domain.ReplyIntent;
import com.recruit.agent.conversation.domain.ReplySuggestion;
import com.recruit.agent.conversation.domain.ReplySuggestionStatus;

import java.time.Instant;

public record ReplySuggestionResponse(
        Long id,
        Long conversationId,
        ReplyIntent intent,
        double confidence,
        boolean needHuman,
        String suggestedReply,
        ReplySuggestionStatus status,
        Instant createdAt
) {

    public static ReplySuggestionResponse from(ReplySuggestion suggestion) {
        return new ReplySuggestionResponse(
                suggestion.id(),
                suggestion.conversationId(),
                suggestion.intent(),
                suggestion.confidence(),
                suggestion.needHuman(),
                suggestion.suggestedReply(),
                suggestion.status(),
                suggestion.createdAt()
        );
    }
}
