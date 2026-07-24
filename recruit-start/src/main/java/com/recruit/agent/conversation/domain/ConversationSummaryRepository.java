package com.recruit.agent.conversation.domain;

import java.util.Optional;

public interface ConversationSummaryRepository {
    Optional<ConversationSummary> findByConversationId(Long conversationId);

    ConversationSummary save(ConversationSummary summary);
}
