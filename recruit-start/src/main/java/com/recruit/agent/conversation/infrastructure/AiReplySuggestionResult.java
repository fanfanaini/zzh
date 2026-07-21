package com.recruit.agent.conversation.infrastructure;

public record AiReplySuggestionResult(
        String intent,
        double confidence,
        boolean needHuman,
        String suggestedReply
) {
}
