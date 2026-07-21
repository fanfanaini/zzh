package com.recruit.agent.conversation.application;

import com.recruit.agent.conversation.domain.MessageSenderType;

public record AddMessageCommand(
        Long conversationId,
        MessageSenderType senderType,
        String content
) {
}
