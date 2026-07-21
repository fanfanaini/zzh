package com.recruit.agent.conversation.api;

import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;

import java.time.Instant;

public record ConversationResponse(
        Long id,
        Long jobId,
        Long candidateId,
        ConversationStage stage,
        ConversationStatus status,
        Instant updatedAt,
        Instant lastCandidateReplyAt,
        Instant nextFollowUpAt,
        Instant createdAt
) {

    public static ConversationResponse from(Conversation conversation) {
        return new ConversationResponse(
                conversation.id(),
                conversation.jobId(),
                conversation.candidateId(),
                conversation.stage(),
                conversation.status(),
                conversation.updatedAt(),
                conversation.lastCandidateReplyAt(),
                conversation.nextFollowUpAt(),
                conversation.createdAt()
        );
    }
}
