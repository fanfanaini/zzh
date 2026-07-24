package com.recruit.agent.conversation.domain;

import java.util.List;

@FunctionalInterface
public interface ConversationSummaryProvider {

    String summarize(Conversation conversation, List<ConversationMessage> messages, int maximumTokens);
}
