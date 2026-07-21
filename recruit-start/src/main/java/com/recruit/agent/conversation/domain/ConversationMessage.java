package com.recruit.agent.conversation.domain;

import java.time.Instant;

public record ConversationMessage(
        Long id,
        Long conversationId,
        MessageSenderType senderType,
        String content,
        Instant createdAt
) implements java.io.Serializable {

    public ConversationMessage {
        if (id == null) {
            throw new IllegalArgumentException("消息 ID 不能为空");
        }
        if (conversationId == null) {
            throw new IllegalArgumentException("会话 ID 不能为空");
        }
        if (senderType == null) {
            throw new IllegalArgumentException("发送方类型不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        content = content.trim();
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
