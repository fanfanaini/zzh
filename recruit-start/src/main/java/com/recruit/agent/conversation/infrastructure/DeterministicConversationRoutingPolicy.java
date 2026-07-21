package com.recruit.agent.conversation.infrastructure;

import com.recruit.agent.conversation.domain.ConversationRoute;
import com.recruit.agent.conversation.domain.ConversationRoutingPolicy;
import com.recruit.agent.conversation.domain.IntentClassificationResult;

public class DeterministicConversationRoutingPolicy implements ConversationRoutingPolicy {

    @Override
    public ConversationRoute route(IntentClassificationResult classification) {
        if (classification == null || classification.handoffRequired() || !classification.risks().isEmpty()) {
            return ConversationRoute.HUMAN_HANDOFF;
        }
        return switch (classification.intent()) {
            case ASK_FAQ -> ConversationRoute.FAQ_RAG;
            case ANSWER_JOB_QUESTION -> ConversationRoute.JOB_INFO;
            case ASK_AVAILABILITY, INVITE_INTERVIEW -> ConversationRoute.INTERVIEW_SCHEDULING;
            case ASK_CANDIDATE_STATUS -> ConversationRoute.CANDIDATE_STATUS;
            case REJECT_CANDIDATE -> ConversationRoute.HUMAN_HANDOFF;
            case OUT_OF_DOMAIN -> ConversationRoute.SAFE_REPLY;
            default -> ConversationRoute.GENERAL_REPLY;
        };
    }
}
