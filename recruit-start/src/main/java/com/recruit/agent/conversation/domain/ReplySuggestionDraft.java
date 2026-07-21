package com.recruit.agent.conversation.domain;

public record ReplySuggestionDraft(
        ReplyIntent intent,
        double confidence,
        boolean needHuman,
        String suggestedReply
) {
}
