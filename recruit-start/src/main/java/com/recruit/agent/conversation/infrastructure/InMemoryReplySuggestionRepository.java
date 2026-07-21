package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.conversation.domain.ReplySuggestion;
import com.recruit.agent.conversation.domain.ReplySuggestionRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class InMemoryReplySuggestionRepository implements ReplySuggestionRepository {

    private final Map<Long, ReplySuggestion> suggestions = new ConcurrentHashMap<>();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    @Override
    public ReplySuggestion save(ReplySuggestion suggestion) {
        suggestions.put(suggestion.id(), suggestion);
        return suggestion;
    }

    @Override
    public Optional<ReplySuggestion> findById(Long id) {
        return Optional.ofNullable(suggestions.get(id));
    }

    @Override
    public List<ReplySuggestion> findByConversationId(Long conversationId) {
        return suggestions.values().stream()
                .filter(suggestion -> suggestion.conversationId().equals(conversationId))
                .sorted(Comparator.comparing(ReplySuggestion::createdAt).reversed())
                .toList();
    }

    @Override
    public long nextSuggestionId() {
        return idGenerator.nextId();
    }
}
