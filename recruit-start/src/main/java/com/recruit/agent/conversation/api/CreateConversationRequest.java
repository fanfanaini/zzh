package com.recruit.agent.conversation.api;

import com.recruit.agent.conversation.domain.ConversationStage;
import jakarta.validation.constraints.NotNull;

public record CreateConversationRequest(
        @NotNull(message = "岗位 ID 不能为空")
        Long jobId,
        @NotNull(message = "候选人 ID 不能为空")
        Long candidateId,
        ConversationStage stage
) {
}
