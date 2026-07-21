package com.recruit.agent.conversation.domain;

import java.util.List;
import java.util.Map;

public record ConversationMemory(
        List<ConversationMessage> recentMessages,
        String summary,
        Map<String, String> facts,
        Map<String, String> state,
        int characterCount,
        boolean truncated
) implements java.io.Serializable {
    public ConversationMemory {
        recentMessages = recentMessages == null ? List.of() : List.copyOf(recentMessages);
        summary = summary == null ? "" : summary.trim();
        facts = facts == null ? Map.of() : Map.copyOf(facts);
        state = state == null ? Map.of() : Map.copyOf(state);
        if (characterCount < 0) {
            throw new IllegalArgumentException("Memory character count cannot be negative");
        }
    }
}
