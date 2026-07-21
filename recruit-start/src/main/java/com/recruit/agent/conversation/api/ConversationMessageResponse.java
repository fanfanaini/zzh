package com.recruit.agent.conversation.api;

import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.MessageSenderType;

import java.time.Instant;

public record ConversationMessageResponse(
        Long id,
        Long conversationId,
        MessageSenderType senderType,
        String content,
        Instant createdAt
) {

    public static ConversationMessageResponse from(ConversationMessage message) {
        return new ConversationMessageResponse(
                message.id(),
                message.conversationId(),
                message.senderType(),
                message.content(),
                message.createdAt()
        );
    }
}
