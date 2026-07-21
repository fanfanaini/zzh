package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.common.id.TimestampIdGenerator;
import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class InMemoryConversationRepository implements ConversationRepository {

    private final Map<Long, Conversation> conversations = new ConcurrentHashMap<>();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    @Override
    public Conversation save(Conversation conversation) {
        conversations.put(conversation.id(), conversation);
        return conversation;
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        return Optional.ofNullable(conversations.get(id));
    }

    @Override
    public List<Conversation> findAll() {
        return conversations.values().stream()
                .sorted(Comparator.comparing(Conversation::createdAt).reversed())
                .toList();
    }

    @Override
    public List<Conversation> findByJobId(Long jobId) {
        return conversations.values().stream()
                .filter(conversation -> conversation.jobId().equals(jobId))
                .sorted(Comparator.comparing(Conversation::createdAt).reversed())
                .toList();
    }

    @Override
    public List<Conversation> findByCandidateId(Long candidateId) {
        return conversations.values().stream()
                .filter(conversation -> conversation.candidateId().equals(candidateId))
                .sorted(Comparator.comparing(Conversation::createdAt).reversed())
                .toList();
    }

    @Override
    public long nextConversationId() {
        return idGenerator.nextId();
    }
}
