package com.recruit.agent.conversation.domain;

import java.util.List;
import java.util.Optional;

public interface ReplySuggestionRepository {

    ReplySuggestion save(ReplySuggestion suggestion);

    Optional<ReplySuggestion> findById(Long id);

    List<ReplySuggestion> findByConversationId(Long conversationId);

    long nextSuggestionId();
}
