package com.recruit.agent.conversation.domain;

public interface ConversationRoutingPolicy {

    ConversationRoute route(IntentClassificationResult classification);
}
