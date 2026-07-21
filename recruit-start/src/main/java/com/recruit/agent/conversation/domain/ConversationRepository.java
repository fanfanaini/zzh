package com.recruit.agent.conversation.domain;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository {

    Conversation save(Conversation conversation);

    Optional<Conversation> findById(Long id);

    List<Conversation> findAll();

    List<Conversation> findByJobId(Long jobId);

    List<Conversation> findByCandidateId(Long candidateId);

    long nextConversationId();
}
