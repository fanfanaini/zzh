package com.recruit.agent.conversation.domain;

import java.util.List;

@FunctionalInterface
public interface ConversationSummaryProvider {

    String summarize(List<ConversationMessage> messages, int maximumCharacters);
}
