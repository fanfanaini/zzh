package com.recruit.agent.conversation.domain;

import java.util.List;

public interface ConversationMessageRepository {

    ConversationMessage save(ConversationMessage message);

    List<ConversationMessage> findByConversationId(Long conversationId);

    long nextMessageId();
}
