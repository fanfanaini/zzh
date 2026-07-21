package com.recruit.agent.conversation.domain;

import java.time.Instant;

public record ReplySuggestion(
        Long id,
        Long conversationId,
        ReplyIntent intent,
        double confidence,
        boolean needHuman,
        String suggestedReply,
        ReplySuggestionStatus status,
        Instant createdAt
) {

    public ReplySuggestion {
        if (id == null) {
            throw new IllegalArgumentException("回复建议 ID 不能为空");
        }
        if (conversationId == null) {
            throw new IllegalArgumentException("会话 ID 不能为空");
        }
        if (intent == null) {
            intent = ReplyIntent.UNKNOWN;
        }
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("置信度必须在 0 到 1 之间");
        }
        if (suggestedReply == null || suggestedReply.isBlank()) {
            throw new IllegalArgumentException("回复草稿不能为空");
        }
        suggestedReply = suggestedReply.trim();
        if (status == null) {
            status = ReplySuggestionStatus.DRAFT;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public ReplySuggestion approve() {
        return withStatus(ReplySuggestionStatus.APPROVED);
    }

    public ReplySuggestion reject() {
        return withStatus(ReplySuggestionStatus.REJECTED);
    }

    private ReplySuggestion withStatus(ReplySuggestionStatus status) {
        return new ReplySuggestion(
                id,
                conversationId,
                intent,
                confidence,
                needHuman,
                suggestedReply,
                status,
                createdAt
        );
    }
}
