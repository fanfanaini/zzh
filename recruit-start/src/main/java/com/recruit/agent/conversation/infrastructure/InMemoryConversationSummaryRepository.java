package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.ConversationSummary;
import com.recruit.agent.conversation.domain.ConversationSummaryRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("!mysql")
public class InMemoryConversationSummaryRepository implements ConversationSummaryRepository {
    private final Map<Long, ConversationSummary> summaries = new ConcurrentHashMap<>();

    @Override
    public Optional<ConversationSummary> findByConversationId(Long conversationId) {
        return Optional.ofNullable(summaries.get(conversationId));
    }

    @Override
    public ConversationSummary save(ConversationSummary summary) {
        summaries.put(summary.conversationId(), summary);
        return summary;
    }
}
