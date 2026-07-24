package com.recruit.agent.conversation.domain;

import java.time.Instant;

public record ConversationSummary(
        Long conversationId,
        String content,
        int summarizedMessageCount,
        Long summarizedThroughMessageId,
        int tokenCount,
        Instant updatedAt
) {
    public ConversationSummary {
        if (conversationId == null) throw new IllegalArgumentException("conversationId must not be null");
        content = content == null ? "" : content.trim();
        if (summarizedMessageCount < 0) throw new IllegalArgumentException("summarizedMessageCount must not be negative");
        if (tokenCount < 0) throw new IllegalArgumentException("tokenCount must not be negative");
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }
}
