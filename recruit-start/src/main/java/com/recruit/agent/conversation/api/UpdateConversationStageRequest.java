package com.recruit.agent.conversation.api;

import com.recruit.agent.conversation.domain.ConversationStage;
import jakarta.validation.constraints.NotNull;

public record UpdateConversationStageRequest(
        @NotNull
        ConversationStage stage
) {
}
