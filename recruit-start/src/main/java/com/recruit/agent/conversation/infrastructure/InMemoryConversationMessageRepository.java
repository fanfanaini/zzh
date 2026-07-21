package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.conversation.domain.ConversationMessage;
import com.recruit.agent.conversation.domain.ConversationMessageRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class InMemoryConversationMessageRepository implements ConversationMessageRepository {

    private final Map<Long, ConversationMessage> messages = new ConcurrentHashMap<>();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    @Override
    public ConversationMessage save(ConversationMessage message) {
        messages.put(message.id(), message);
        return message;
    }

    @Override
    public List<ConversationMessage> findByConversationId(Long conversationId) {
        return messages.values().stream()
                .filter(message -> message.conversationId().equals(conversationId))
                .sorted(Comparator.comparing(ConversationMessage::createdAt)
                        .thenComparing(ConversationMessage::id))
                .toList();
    }

    @Override
    public long nextMessageId() {
        return idGenerator.nextId();
    }
}
