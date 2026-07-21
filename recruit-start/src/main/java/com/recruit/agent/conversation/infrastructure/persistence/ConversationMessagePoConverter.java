package com.recruit.agent.conversation.infrastructure.persistence;

import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.MessageSenderType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ConversationMessagePoConverter {

    public ConversationMessagePo toPo(ConversationMessage message) {
        ConversationMessagePo po = new ConversationMessagePo();
        po.setId(message.id());
        po.setConversationId(message.conversationId());
        po.setSenderType(message.senderType().name());
        po.setContent(message.content());
        po.setCreatedAt(LocalDateTime.ofInstant(message.createdAt(), ZoneOffset.UTC));
        return po;
    }

    public ConversationMessage toDomain(ConversationMessagePo po) {
        return new ConversationMessage(
                po.getId(),
                po.getConversationId(),
                MessageSenderType.valueOf(po.getSenderType()),
                po.getContent(),
                po.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
