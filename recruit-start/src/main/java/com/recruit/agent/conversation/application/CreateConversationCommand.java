package com.recruit.agent.conversation.application;

import com.recruit.agent.conversation.domain.ConversationStage;

public record CreateConversationCommand(
        Long jobId,
        Long candidateId,
        ConversationStage stage
) {
}
