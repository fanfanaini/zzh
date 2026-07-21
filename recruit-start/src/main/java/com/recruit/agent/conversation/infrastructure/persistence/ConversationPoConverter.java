package com.recruit.agent.conversation.infrastructure.persistence;

import com.recruit.agent.conversation.domain.Conversation;
import com.recruit.agent.conversation.domain.ConversationStage;
import com.recruit.agent.conversation.domain.ConversationStatus;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ConversationPoConverter {

    public ConversationPo toPo(Conversation conversation) {
        ConversationPo po = new ConversationPo();
        po.setId(conversation.id());
        po.setJobId(conversation.jobId());
        po.setCandidateId(conversation.candidateId());
        po.setStage(conversation.stage().name());
        po.setStatus(conversation.status().name());
        po.setCreatedAt(LocalDateTime.ofInstant(conversation.createdAt(), ZoneOffset.UTC));
        po.setUpdatedAt(LocalDateTime.ofInstant(conversation.updatedAt(), ZoneOffset.UTC));
        po.setLastCandidateReplyAt(conversation.lastCandidateReplyAt() == null ? null
                : LocalDateTime.ofInstant(conversation.lastCandidateReplyAt(), ZoneOffset.UTC));
        po.setNextFollowUpAt(conversation.nextFollowUpAt() == null ? null
                : LocalDateTime.ofInstant(conversation.nextFollowUpAt(), ZoneOffset.UTC));
        return po;
    }

    public Conversation toDomain(ConversationPo po) {
        return new Conversation(
                po.getId(),
                po.getJobId(),
                po.getCandidateId(),
                ConversationStage.valueOf(po.getStage()),
                ConversationStatus.valueOf(po.getStatus()),
                po.getCreatedAt().toInstant(ZoneOffset.UTC),
                po.getUpdatedAt() == null ? po.getCreatedAt().toInstant(ZoneOffset.UTC) : po.getUpdatedAt().toInstant(ZoneOffset.UTC),
                po.getLastCandidateReplyAt() == null ? null : po.getLastCandidateReplyAt().toInstant(ZoneOffset.UTC),
                po.getNextFollowUpAt() == null ? null : po.getNextFollowUpAt().toInstant(ZoneOffset.UTC)
        );
    }
}
