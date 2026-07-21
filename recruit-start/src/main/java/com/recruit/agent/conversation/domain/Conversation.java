package com.recruit.agent.conversation.domain;

import java.time.Instant;

public record Conversation(
        Long id,
        Long jobId,
        Long candidateId,
        ConversationStage stage,
        ConversationStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant lastCandidateReplyAt,
        Instant nextFollowUpAt
) {

    public Conversation(Long id, Long jobId, Long candidateId, ConversationStage stage,
                        ConversationStatus status, Instant createdAt) {
        this(id, jobId, candidateId, stage, status, createdAt, createdAt, null, null);
    }

    public Conversation {
        if (id == null) {
            throw new IllegalArgumentException("会话 ID 不能为空");
        }
        if (jobId == null) {
            throw new IllegalArgumentException("岗位 ID 不能为空");
        }
        if (candidateId == null) {
            throw new IllegalArgumentException("候选人 ID 不能为空");
        }
        if (stage == null) {
            stage = ConversationStage.INITIAL_CONTACT;
        }
        if (status == null) {
            status = ConversationStatus.OPEN;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    public Conversation updateStage(ConversationStage newStage) {
        return new Conversation(id, jobId, candidateId, newStage, status, createdAt, Instant.now(),
                lastCandidateReplyAt, nextFollowUpAt);
    }

    public Conversation close() {
        return new Conversation(id, jobId, candidateId, stage, ConversationStatus.CLOSED, createdAt, Instant.now(),
                lastCandidateReplyAt, nextFollowUpAt);
    }

    public Conversation markCandidateReplied() {
        Instant now = Instant.now();
        return new Conversation(id, jobId, candidateId, stage, status, createdAt, now, now, nextFollowUpAt);
    }

    public Conversation scheduleFollowUp(Instant followUpAt) {
        return new Conversation(id, jobId, candidateId, stage, status, createdAt, Instant.now(),
                lastCandidateReplyAt, followUpAt);
    }
}
